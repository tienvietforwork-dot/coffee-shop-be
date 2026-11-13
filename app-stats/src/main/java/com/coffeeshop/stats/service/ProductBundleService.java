package com.coffeeshop.stats.service;

import com.coffeeshop.stats.dto.request.ProductBundleItemRequest;
import com.coffeeshop.stats.dto.request.ProductBundleRequest;
import com.coffeeshop.stats.dto.response.ProductBundleAnalyticsResponse;
import com.coffeeshop.stats.dto.response.ProductBundleResponse;
import com.coffeeshop.stats.entity.Coffee;
import com.coffeeshop.stats.entity.ProductBundle;
import com.coffeeshop.stats.entity.ProductBundleItem;
import com.coffeeshop.stats.entity.enums.CoffeeStatus;
import com.coffeeshop.stats.entity.enums.CategoryProductType;
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
import com.coffeeshop.stats.util.ShopTime;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
        LocalDateTime now = ShopTime.now();
        return bundleRepository.findAllByOrderByStatusAscNameAsc()
                .stream()
                .map(bundle -> ProductBundleResponse.from(bundle, now))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductBundleResponse> findPublicBundles() {
        LocalDateTime now = ShopTime.now();
        return bundleRepository.findAllByOrderByStatusAscNameAsc().stream()
                .filter(bundle -> bundle.isAvailableNow(now))
                .filter(this::hasSellableConfiguration)
                .map(bundle -> ProductBundleResponse.from(bundle, now)).toList();
    }

    @Transactional(readOnly = true)
    public ProductBundleResponse findById(Long id) {
        return ProductBundleResponse.from(getBundle(id), ShopTime.now());
    }

    @Transactional
    public ProductBundleResponse create(ProductBundleRequest request) {
        validateSchedule(request.startsAt(), request.endsAt());
        validateBundleRequest(request);
        if (bundleRepository.existsByNameIgnoreCase(request.name().trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Combo đã tồn tại: " + request.name());
        }

        ProductBundle bundle = new ProductBundle();
        apply(bundle, request);
        replaceItems(bundle, request.items());
        return ProductBundleResponse.from(bundleRepository.save(bundle), ShopTime.now());
    }

    @Transactional
    public ProductBundleResponse update(Long id, ProductBundleRequest request) {
        validateSchedule(request.startsAt(), request.endsAt());
        validateBundleRequest(request);
        ProductBundle bundle = getBundle(id);
        if (bundleRepository.existsByNameIgnoreCaseAndIdNot(request.name().trim(), id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Combo đã tồn tại: " + request.name());
        }

        if (bundle.getStatus() == ProductBundleStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tạm dừng combo trước khi sửa");
        }
        apply(bundle, request);
        // An omitted items field means metadata-only update, not remove every item.
        if (request.items() != null) {
            replaceItems(bundle, request.items());
        }
        return ProductBundleResponse.from(bundle, ShopTime.now());
    }

    @Transactional
    public ProductBundleResponse addItem(Long bundleId, ProductBundleItemRequest request) {
        ProductBundle bundle = getBundle(bundleId);
        requireEditable(bundle);
        if (bundle.getItems().size() >= 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mỗi combo chỉ gồm đúng 2 loại cà phê khác nhau");
        }
        if (bundleItemRepository.existsByBundle_IdAndCoffee_Id(bundleId, request.coffeeId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sản phẩm đã có trong combo");
        }
        bundle.getItems().add(toItem(bundle, request));
        return ProductBundleResponse.from(bundle, ShopTime.now());
    }

    @Transactional
    public ProductBundleResponse updateItem(Long bundleId, Long itemId, ProductBundleItemRequest request) {
        ProductBundle bundle = getBundle(bundleId);
        requireEditable(bundle);
        ProductBundleItem item = findItem(bundle, itemId);
        if (bundleItemRepository.existsByBundle_IdAndCoffee_IdAndIdNot(bundleId, request.coffeeId(), itemId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sản phẩm đã có trong combo");
        }
        applyItem(item, request);
        return ProductBundleResponse.from(bundle, ShopTime.now());
    }

    @Transactional
    public ProductBundleResponse removeItem(Long bundleId, Long itemId) {
        ProductBundle bundle = getBundle(bundleId);
        requireEditable(bundle);
        ProductBundleItem item = findItem(bundle, itemId);
        item.softDelete();
        bundle.getItems().remove(item);
        return ProductBundleResponse.from(bundle, ShopTime.now());
    }

    @Transactional
    public ProductBundleResponse activate(Long id) {
        ProductBundle bundle = getBundle(id);
        if (bundle.getBundlePrice() == null || bundle.getBundlePrice().signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giá combo phải lớn hơn 0");
        }
        validateSchedule(bundle.getStartsAt(), bundle.getEndsAt());
        if (bundle.getEndsAt() != null && !bundle.getEndsAt().isAfter(ShopTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể kích hoạt combo đã hết hạn");
        }
        validateCoffeeOnly(bundle.getItems());
        validateExactlyTwoCoffees(bundle.getItems());
        List<ProductBundleItem> unavailableRequired = bundle.getItems().stream()
                .filter(item -> !isSellable(item.getCoffee())).toList();
        if (!unavailableRequired.isEmpty()) {
            String names = unavailableRequired.stream().map(item -> item.getCoffee().getName()).distinct()
                    .collect(Collectors.joining(", "));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Món bắt buộc đang hết hàng/ẩn: " + names + ". Hãy thay món hoặc bổ sung tồn kho trước khi kích hoạt.");
        }
        validateMandatoryPrice(bundle);
        bundle.setStatus(ProductBundleStatus.ACTIVE);
        return ProductBundleResponse.from(bundle, ShopTime.now());
    }

    @Transactional
    public ProductBundleResponse pause(Long id) {
        ProductBundle bundle = getBundle(id);
        bundle.setStatus(ProductBundleStatus.PAUSED);
        return ProductBundleResponse.from(bundle, ShopTime.now());
    }

    @Transactional
    public void delete(Long id) {
        ProductBundle bundle = getBundle(id);
        bundle.getItems().forEach(ProductBundleItem::softDelete);
        bundle.softDelete();
    }

    @Transactional(readOnly = true)
    public ProductBundleAnalyticsResponse analytics(Long id, LocalDateTime from, LocalDateTime to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Khoảng thời gian thống kê không hợp lệ");
        }
        ProductBundle bundle = getBundle(id);
        List<ProductBundleItem> items = bundle.getItems();
        // Component totals originate only from actual sold combo records, not independent item sales.
        Map<Long, SalesSummary> sales = new HashMap<>();
        StringBuilder componentSql = new StringBuilder("SELECT c.coffee_id,SUM(c.quantity),SUM(c.line_total) "
                + "FROM order_bundle_components c JOIN order_bundle_sales s ON s.id=c.sale_id "
                + "JOIN orders o ON o.id=s.order_id WHERE s.bundle_id=:bundleId "
                + "AND o.status='COMPLETED' AND o.del_flag=false");
        if(from!=null) componentSql.append(" AND o.ordered_at>=:fromTime");
        if(to!=null) componentSql.append(" AND o.ordered_at<=:toTime");
        componentSql.append(" GROUP BY c.coffee_id");
        var componentQuery=entityManager.createNativeQuery(componentSql.toString()).setParameter("bundleId",id);
        if(from!=null) componentQuery.setParameter("fromTime",from);
        if(to!=null) componentQuery.setParameter("toTime",to);
        for(Object raw:componentQuery.getResultList()) {
            Object[] row=(Object[])raw;
            sales.put(((Number)row[0]).longValue(),new SalesSummary(((Number)row[1]).intValue(),(BigDecimal)row[2]));
        }

        // Real completed combo purchases only. Regular coffee orders must not be counted as combo sales.
        StringBuilder realSql = new StringBuilder("SELECT COALESCE(SUM(s.quantity),0), COALESCE(SUM(s.quantity*s.unit_price+s.optional_total),0) "
                + "FROM order_bundle_sales s JOIN orders o ON o.id=s.order_id "
                + "WHERE s.bundle_id=:bundleId AND o.status='COMPLETED' AND o.del_flag=false");
        if (from != null) realSql.append(" AND o.ordered_at >= :fromTime");
        if (to != null) realSql.append(" AND o.ordered_at <= :toTime");
        var query = entityManager.createNativeQuery(realSql.toString()).setParameter("bundleId", id);
        if (from != null) query.setParameter("fromTime", from);
        if (to != null) query.setParameter("toTime", to);
        Object[] totals = (Object[]) query.getSingleResult();
        int estimatedSoldQuantity = ((Number) totals[0]).intValue();
        BigDecimal realRevenue = (BigDecimal) totals[1];

        BigDecimal standalonePrice = standalonePrice(bundle);
        BigDecimal estimatedRevenue = realRevenue;
        BigDecimal estimatedSavingGiven = standalonePrice.subtract(bundle.getBundlePrice())
                .multiply(BigDecimal.valueOf(estimatedSoldQuantity));

        List<ProductBundleAnalyticsResponse.ComponentSales> components = items.stream()
                .map(item -> {
                    SalesSummary summary = sales.getOrDefault(item.getCoffee().getId(), SalesSummary.ZERO);
                    return new ProductBundleAnalyticsResponse.ComponentSales(item.getCoffee().getId(),
                            item.getCoffee().getName(), item.getQuantity(), item.isOptional(), item.getChoiceGroup(),
                            summary.quantity(), summary.revenue());
                })
                .toList();

        return new ProductBundleAnalyticsResponse(bundle.getId(), bundle.getName(), from, to, estimatedSoldQuantity,
                estimatedRevenue, standalonePrice, bundle.getBundlePrice(), estimatedSavingGiven, components);
    }

    private void apply(ProductBundle bundle, ProductBundleRequest request) {
        bundle.setName(request.name().trim());
        bundle.setDescription(blankToNull(request.description()));
        bundle.setImageId(request.imageId());
        bundle.setImageUrl(request.imageId() == null ? blankToNull(request.imageUrl()) : null);
        bundle.setBundlePrice(request.bundlePrice());
        bundle.setStartsAt(request.startsAt());
        bundle.setEndsAt(request.endsAt());
        // Status changes must pass through activate/pause APIs, never bypass activation validation.
        if (bundle.getId() == null || request.status() == null || request.status() == ProductBundleStatus.DRAFT) {
            bundle.setStatus(ProductBundleStatus.DRAFT);
        } else if (request.status() != bundle.getStatus()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dùng API activate/pause để đổi trạng thái combo");
        }
    }

    /** The fixed combo price includes both coffee components. */
    private void validateMandatoryPrice(ProductBundle bundle) {
        BigDecimal regularPrice = standalonePrice(bundle);
        if (bundle.getBundlePrice().compareTo(regularPrice) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Giá combo không được vượt tổng giá bán lẻ của 2 loại cà phê");
        }
    }

    private BigDecimal standalonePrice(ProductBundle bundle) {
        return bundle.getItems().stream()
                .map(item -> item.getCoffee().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void requireEditable(ProductBundle bundle) {
        if (bundle.getStatus() == ProductBundleStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tạm dừng combo trước khi thay đổi món");
        }
    }

    private void validateBundleRequest(ProductBundleRequest request) {
        if (request.imageId() == null && blankToNull(request.imageUrl()) == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Combo cần có ảnh trước khi lưu");
        }
        if (request.imageId() != null) {
            Number imageCount = (Number) entityManager.createNativeQuery("SELECT count(*) FROM images WHERE id = :id")
                    .setParameter("id", request.imageId()).getSingleResult();
            if (imageCount.longValue() == 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ảnh combo không tồn tại");
            }
        }
        if (request.items() != null) {
            if (request.items().size() > 2) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Mỗi combo chỉ gồm đúng 2 loại cà phê khác nhau");
            }
            Set<Long> ids = new LinkedHashSet<>();
            for (ProductBundleItemRequest item : request.items()) {
                if (item == null || item.coffeeId() == null || item.quantity() == null
                        || item.quantity() < 1 || item.quantity() > 99 || item.coffeeId() <= 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thành phần combo không hợp lệ");
                }
                if (!ids.add(item.coffeeId())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không được lặp sản phẩm trong combo");
                }
            }
        }
        if (request.status() == ProductBundleStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dùng API activate sau khi tạo combo DRAFT");
        }
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
        Coffee coffee = getCoffee(request.coffeeId());
        if (request.quantity() == null || request.quantity() < 1 || request.quantity() > 99) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượng mỗi thành phần phải từ 1 đến 99");
        }
        if (!isSellable(coffee)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    coffee.getName() + " hiện không còn bán được, không thể thêm vào combo");
        }
        if (!isDrink(coffee)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    coffee.getName() + " không phải cà phê/đồ uống; combo chỉ được gồm các món uống");
        }
        if (Boolean.TRUE.equals(request.optional()) || blankToNull(request.choiceGroup()) != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Combo chỉ gồm đúng 2 loại cà phê bắt buộc, không hỗ trợ món tùy chọn hoặc món thêm");
        }
        item.setCoffee(coffee);
        item.setQuantity(request.quantity());
        item.setOptional(false);
        item.setChoiceGroup(null);
        item.setDisplayOrder(request.displayOrder() == null ? 0 : request.displayOrder());
    }

    private boolean hasSellableConfiguration(ProductBundle bundle) {
        if (bundle.getItems().size() != 2 || bundle.getItems().stream().anyMatch(item -> !isDrink(item.getCoffee())
                || item.isOptional() || blankToNull(item.getChoiceGroup()) != null
                || !isSellable(item.getCoffee()))) return false;
        if (bundle.getItems().stream().map(item -> item.getCoffee().getId()).distinct().count() != 2) return false;
        return bundle.getBundlePrice().compareTo(standalonePrice(bundle)) <= 0;
    }

    private void validateExactlyTwoCoffees(List<ProductBundleItem> items) {
        if (items.size() != 2 || items.stream().map(item -> item.getCoffee().getId()).distinct().count() != 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Combo phải gồm đúng 2 loại cà phê khác nhau; hãy thêm hoặc xóa thành phần cho đủ 2 món");
        }
    }

    private void validateCoffeeOnly(List<ProductBundleItem> items) {
        for (ProductBundleItem item : items) {
            if (!isDrink(item.getCoffee())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        item.getCoffee().getName() + " không phải cà phê/đồ uống; hãy xóa món này khỏi combo");
            }
            if (item.isOptional() || blankToNull(item.getChoiceGroup()) != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Combo chỉ gồm các cà phê bắt buộc; hãy bỏ lựa chọn thay thế hoặc món thêm khỏi combo");
            }
        }
    }

    private boolean isDrink(Coffee coffee) {
        return coffee.getCategory() != null && coffee.getCategory().getProductType() == CategoryProductType.DRINK;
    }

    private boolean isSellable(Coffee coffee) {
        return coffee.getStatus() == CoffeeStatus.AVAILABLE && !coffee.isAutoSoldOut();
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
        if (startsAt != null && endsAt != null && !endsAt.isAfter(startsAt)) {
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
