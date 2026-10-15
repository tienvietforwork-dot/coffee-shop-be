package com.coffeeshop.stats.service;

import com.coffeeshop.stats.dto.request.ProductBundleItemRequest;
import com.coffeeshop.stats.dto.request.ProductBundleRequest;
import com.coffeeshop.stats.dto.response.ProductBundleAnalyticsResponse;
import com.coffeeshop.stats.dto.response.ProductBundleResponse;
import com.coffeeshop.stats.entity.Coffee;
import com.coffeeshop.stats.entity.ProductBundle;
import com.coffeeshop.stats.entity.ProductBundleItem;
import com.coffeeshop.stats.entity.enums.ProductBundleStatus;
import com.coffeeshop.stats.entity.repository.CoffeeRepository;
import com.coffeeshop.stats.repository.ProductBundleItemRepository;
import com.coffeeshop.stats.repository.ProductBundleRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProductBundleService {

    private final ProductBundleRepository bundleRepository;
    private final ProductBundleItemRepository bundleItemRepository;
    private final CoffeeRepository coffeeRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<ProductBundleResponse> findAll() {
        LocalDateTime now = LocalDateTime.now();
        return bundleRepository.findAllByOrderByStatusAscNameAsc()
                .stream()
                .map(bundle -> ProductBundleResponse.from(bundle, now))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductBundleResponse findById(Long id) {
        return ProductBundleResponse.from(getBundle(id), LocalDateTime.now());
    }

    @Transactional
    public ProductBundleResponse create(ProductBundleRequest request) {
        validateSchedule(request.startsAt(), request.endsAt());
        if (bundleRepository.existsByNameIgnoreCase(request.name().trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Combo đã tồn tại: " + request.name());
        }

        ProductBundle bundle = new ProductBundle();
        apply(bundle, request);
        replaceItems(bundle, request.items());
        return ProductBundleResponse.from(bundleRepository.save(bundle), LocalDateTime.now());
    }

    @Transactional
    public ProductBundleResponse update(Long id, ProductBundleRequest request) {
        validateSchedule(request.startsAt(), request.endsAt());
        ProductBundle bundle = getBundle(id);
        if (bundleRepository.existsByNameIgnoreCaseAndIdNot(request.name().trim(), id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Combo đã tồn tại: " + request.name());
        }

        apply(bundle, request);
        replaceItems(bundle, request.items());
        return ProductBundleResponse.from(bundle, LocalDateTime.now());
    }

    @Transactional
    public ProductBundleResponse addItem(Long bundleId, ProductBundleItemRequest request) {
        ProductBundle bundle = getBundle(bundleId);
        if (bundleItemRepository.existsByBundle_IdAndCoffee_Id(bundleId, request.coffeeId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sản phẩm đã có trong combo");
        }
        bundle.getItems().add(toItem(bundle, request));
        return ProductBundleResponse.from(bundle, LocalDateTime.now());
    }

    @Transactional
    public ProductBundleResponse updateItem(Long bundleId, Long itemId, ProductBundleItemRequest request) {
        ProductBundle bundle = getBundle(bundleId);
        ProductBundleItem item = findItem(bundle, itemId);
        if (bundleItemRepository.existsByBundle_IdAndCoffee_IdAndIdNot(bundleId, request.coffeeId(), itemId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sản phẩm đã có trong combo");
        }
        applyItem(item, request);
        return ProductBundleResponse.from(bundle, LocalDateTime.now());
    }

    @Transactional
    public ProductBundleResponse removeItem(Long bundleId, Long itemId) {
        ProductBundle bundle = getBundle(bundleId);
        ProductBundleItem item = findItem(bundle, itemId);
        item.softDelete();
        bundle.getItems().remove(item);
        return ProductBundleResponse.from(bundle, LocalDateTime.now());
    }

    @Transactional
    public ProductBundleResponse activate(Long id) {
        ProductBundle bundle = getBundle(id);
        if (bundle.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Combo cần ít nhất 1 sản phẩm");
        }
        bundle.setStatus(ProductBundleStatus.ACTIVE);
        return ProductBundleResponse.from(bundle, LocalDateTime.now());
    }

    @Transactional
    public ProductBundleResponse pause(Long id) {
        ProductBundle bundle = getBundle(id);
        bundle.setStatus(ProductBundleStatus.PAUSED);
        return ProductBundleResponse.from(bundle, LocalDateTime.now());
    }

    @Transactional
    public void delete(Long id) {
        ProductBundle bundle = getBundle(id);
        bundle.getItems().forEach(ProductBundleItem::softDelete);
        bundle.softDelete();
    }

    @Transactional(readOnly = true)
    public ProductBundleAnalyticsResponse analytics(Long id, LocalDateTime from, LocalDateTime to) {
        ProductBundle bundle = getBundle(id);
        List<ProductBundleItem> items = bundle.getItems();
        Map<Long, SalesSummary> sales = salesByCoffee(items.stream().map(i -> i.getCoffee().getId()).toList(), from, to);

        int estimatedSoldQuantity = items.stream()
                .filter(item -> !item.isOptional())
                .mapToInt(item -> sales.getOrDefault(item.getCoffee().getId(), SalesSummary.ZERO).quantity() / item.getQuantity())
                .min()
                .orElse(0);

        BigDecimal standalonePrice = items.stream()
                .map(item -> item.getCoffee().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal estimatedRevenue = bundle.getBundlePrice().multiply(BigDecimal.valueOf(estimatedSoldQuantity));
        BigDecimal estimatedSavingGiven = standalonePrice.subtract(bundle.getBundlePrice())
                .multiply(BigDecimal.valueOf(estimatedSoldQuantity));

        List<ProductBundleAnalyticsResponse.ComponentSales> components = items.stream()
                .map(item -> {
                    SalesSummary summary = sales.getOrDefault(item.getCoffee().getId(), SalesSummary.ZERO);
                    return new ProductBundleAnalyticsResponse.ComponentSales(item.getCoffee().getId(),
                            item.getCoffee().getName(), item.getQuantity(), item.isOptional(),
                            summary.quantity(), summary.revenue());
                })
                .toList();

        return new ProductBundleAnalyticsResponse(bundle.getId(), bundle.getName(), from, to, estimatedSoldQuantity,
                estimatedRevenue, standalonePrice, bundle.getBundlePrice(), estimatedSavingGiven, components);
    }

    private void apply(ProductBundle bundle, ProductBundleRequest request) {
        bundle.setName(request.name().trim());
        bundle.setDescription(blankToNull(request.description()));
        bundle.setBundlePrice(request.bundlePrice());
        bundle.setStartsAt(request.startsAt());
        bundle.setEndsAt(request.endsAt());
        bundle.setStatus(request.status() == null ? ProductBundleStatus.DRAFT : request.status());
    }

    private void replaceItems(ProductBundle bundle, List<ProductBundleItemRequest> requests) {
        bundle.getItems().forEach(ProductBundleItem::softDelete);
        bundle.getItems().clear();
        if (requests == null) {
            return;
        }

        Set<Long> coffeeIds = new LinkedHashSet<>();
        for (ProductBundleItemRequest request : requests) {
            if (!coffeeIds.add(request.coffeeId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Một sản phẩm không được lặp lại trong cùng combo: " + request.coffeeId());
            }
            bundle.getItems().add(toItem(bundle, request));
        }
    }

    private ProductBundleItem toItem(ProductBundle bundle, ProductBundleItemRequest request) {
        ProductBundleItem item = new ProductBundleItem();
        item.setBundle(bundle);
        applyItem(item, request);
        return item;
    }

    private void applyItem(ProductBundleItem item, ProductBundleItemRequest request) {
        item.setCoffee(getCoffee(request.coffeeId()));
        item.setQuantity(request.quantity());
        item.setOptional(request.optional() != null && request.optional());
        item.setDisplayOrder(request.displayOrder() == null ? 0 : request.displayOrder());
    }

    private ProductBundle getBundle(Long id) {
        return bundleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy combo: " + id));
    }

    private ProductBundleItem findItem(ProductBundle bundle, Long itemId) {
        return bundle.getItems().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy sản phẩm trong combo: " + itemId));
    }

    private Coffee getCoffee(Long id) {
        return coffeeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy sản phẩm: " + id));
    }

    private void validateSchedule(LocalDateTime startsAt, LocalDateTime endsAt) {
        if (startsAt != null && endsAt != null && endsAt.isBefore(startsAt)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Thời gian kết thúc phải sau thời gian bắt đầu");
        }
    }

    private Map<Long, SalesSummary> salesByCoffee(List<Long> coffeeIds, LocalDateTime from, LocalDateTime to) {
        if (coffeeIds.isEmpty()) {
            return Map.of();
        }
        List<?> rows = entityManager.createNativeQuery("""
                        select oi.coffee_id,
                               coalesce(sum(oi.quantity), 0) as total_quantity,
                               coalesce(sum(oi.line_total), 0) as total_revenue
                        from order_items oi
                        join orders o on o.id = oi.order_id
                        where oi.coffee_id in (:coffeeIds)
                          and oi.del_flag = false
                          and o.del_flag = false
                          and o.status = 'COMPLETED'
                          and (:fromDate is null or o.ordered_at >= :fromDate)
                          and (:toDate is null or o.ordered_at <= :toDate)
                        group by oi.coffee_id
                        """)
                .setParameter("coffeeIds", coffeeIds)
                .setParameter("fromDate", from)
                .setParameter("toDate", to)
                .getResultList();

        Map<Long, SalesSummary> result = new HashMap<>();
        for (Object row : rows) {
            Object[] columns = (Object[]) row;
            Long coffeeId = asLong(columns[0]);
            Integer quantity = asInteger(columns[1]);
            BigDecimal revenue = (BigDecimal) columns[2];
            result.put(coffeeId, new SalesSummary(quantity, revenue));
        }
        return result;
    }

    private Long asLong(Object value) {
        if (value instanceof BigInteger bigInteger) {
            return bigInteger.longValue();
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(value.toString());
    }

    private Integer asInteger(Object value) {
        if (value instanceof BigInteger bigInteger) {
            return bigInteger.intValue();
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.valueOf(value.toString());
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private record SalesSummary(Integer quantity, BigDecimal revenue) {
        private static final SalesSummary ZERO = new SalesSummary(0, BigDecimal.ZERO);
    }
}
