package com.coffeeshop.stats.service;

import com.coffeeshop.stats.dto.request.CategoryPriorityRequest;
import com.coffeeshop.stats.dto.request.CoffeeVisibilityRequest;
import com.coffeeshop.stats.dto.request.ProductPlacementRequest;
import com.coffeeshop.stats.dto.response.CategoryPlacementResponse;
import com.coffeeshop.stats.dto.response.CoffeePlacementResponse;
import com.coffeeshop.stats.dto.response.ProductPlacementOverviewResponse;
import com.coffeeshop.stats.dto.response.ProductPlacementResponse;
import com.coffeeshop.stats.entity.Category;
import com.coffeeshop.stats.entity.Coffee;
import com.coffeeshop.stats.entity.ProductPlacementRule;
import com.coffeeshop.stats.entity.enums.CoffeeStatus;
import com.coffeeshop.stats.entity.enums.ProductPlacementSection;
import com.coffeeshop.stats.entity.enums.PublicationStatus;
import com.coffeeshop.stats.entity.repository.CategoryRepository;
import com.coffeeshop.stats.entity.repository.CoffeeRepository;
import com.coffeeshop.stats.entity.repository.ProductPlacementRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductPlacementService {

    private final ProductPlacementRuleRepository placementRuleRepository;
    private final CoffeeRepository coffeeRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public ProductPlacementOverviewResponse overview() {
        LocalDateTime now = LocalDateTime.now();
        Map<ProductPlacementSection, List<ProductPlacementResponse>> sections = placementRuleRepository
                .findAllByOrderBySectionAscDisplayOrderAscIdAsc()
                .stream()
                .map(rule -> ProductPlacementResponse.from(rule, now))
                .collect(Collectors.groupingBy(ProductPlacementResponse::section,
                        () -> new EnumMap<>(ProductPlacementSection.class), Collectors.toList()));

        List<CategoryPlacementResponse> categories = categoryRepository.findAllByOrderByDisplayOrderAscNameAsc()
                .stream()
                .map(CategoryPlacementResponse::from)
                .toList();

        List<CoffeePlacementResponse> coffees = coffeeRepository.findAllByOrderByNameAsc()
                .stream()
                .map(CoffeePlacementResponse::from)
                .toList();

        return new ProductPlacementOverviewResponse(sections, categories, coffees);
    }

    @Transactional(readOnly = true)
    public List<ProductPlacementResponse> findRules(ProductPlacementSection section, boolean visibleOnly) {
        LocalDateTime now = LocalDateTime.now();
        return placementRuleRepository.findAllByOrderBySectionAscDisplayOrderAscIdAsc()
                .stream()
                .filter(rule -> section == null || rule.getSection() == section)
                .filter(rule -> !visibleOnly || rule.isVisibleNow(now))
                .map(rule -> ProductPlacementResponse.from(rule, now))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductPlacementResponse> findPublicRules(ProductPlacementSection section) {
        LocalDateTime now = LocalDateTime.now();
        return placementRuleRepository.findAllByOrderBySectionAscDisplayOrderAscIdAsc().stream()
                .filter(rule -> section == null || rule.getSection() == section)
                .filter(rule -> rule.isVisibleNow(now))
                .filter(rule -> rule.getCoffee().getStatus() == CoffeeStatus.AVAILABLE && !rule.getCoffee().isAutoSoldOut())
                .map(rule -> ProductPlacementResponse.from(rule, now)).toList();
    }

    @Transactional
    public ProductPlacementResponse create(ProductPlacementRequest request) {
        validateSchedule(request.startsAt(), request.endsAt());
        if (placementRuleRepository.existsByCoffee_IdAndSection(request.coffeeId(), request.section())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Sản phẩm đã có trong khu vực " + request.section());
        }

        ProductPlacementRule rule = new ProductPlacementRule();
        apply(rule, request);
        return ProductPlacementResponse.from(placementRuleRepository.save(rule), LocalDateTime.now());
    }

    @Transactional
    public ProductPlacementResponse update(Long id, ProductPlacementRequest request) {
        validateSchedule(request.startsAt(), request.endsAt());
        ProductPlacementRule rule = getRule(id);
        if (placementRuleRepository.existsByCoffee_IdAndSectionAndIdNot(request.coffeeId(), request.section(), id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Sản phẩm đã có trong khu vực " + request.section());
        }

        if (rule.getPublicationStatus() == PublicationStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cần thu hồi cấu hình đã công bố trước khi sửa");
        }
        apply(rule, request);
        return ProductPlacementResponse.from(rule, LocalDateTime.now());
    }

    @Transactional
    public ProductPlacementResponse publish(Long id) {
        ProductPlacementRule rule = getRule(id);
        if (rule.getCoffee().getStatus() != CoffeeStatus.AVAILABLE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ công bố sản phẩm đang bán");
        }
        if (rule.getEndsAt() != null && !rule.getEndsAt().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không công bố cấu hình đã hết hạn");
        }
        rule.setPublicationStatus(PublicationStatus.PUBLISHED);
        rule.setActive(true);
        return ProductPlacementResponse.from(rule, LocalDateTime.now());
    }

    @Transactional
    public ProductPlacementResponse unpublish(Long id) {
        ProductPlacementRule rule = getRule(id);
        rule.setPublicationStatus(PublicationStatus.DRAFT);
        return ProductPlacementResponse.from(rule, LocalDateTime.now());
    }

    @Transactional
    public void delete(Long id) {
        getRule(id).softDelete();
    }

    @Transactional
    public CoffeePlacementResponse updateCoffeeVisibility(Long coffeeId, CoffeeVisibilityRequest request) {
        Coffee coffee = getCoffee(coffeeId);
        CoffeeStatus status = request.status();
        if (status == CoffeeStatus.DISCONTINUED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Product Placement chỉ dùng để ẩn/hiện sản phẩm, không ngừng bán vĩnh viễn");
        }
        coffee.setStatus(status);
        // Do not clear autoSoldOut: warehouse reconciliation owns that flag.
        return CoffeePlacementResponse.from(coffee);
    }

    @Transactional
    public CategoryPlacementResponse updateCategoryPriority(Long categoryId, CategoryPriorityRequest request) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy danh mục: " + categoryId));
        category.setDisplayOrder(request.displayOrder());
        return CategoryPlacementResponse.from(category);
    }

    private void apply(ProductPlacementRule rule, ProductPlacementRequest request) {
        rule.setCoffee(getCoffee(request.coffeeId()));
        rule.setSection(request.section());
        rule.setTitle(blankToNull(request.title()));
        rule.setDisplayOrder(request.displayOrder() == null ? 0 : request.displayOrder());
        rule.setStartsAt(request.startsAt());
        rule.setEndsAt(request.endsAt());
        rule.setActive(request.active() == null || request.active());
        if (request.publicationStatus() == PublicationStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dùng API publish để công bố cấu hình");
        }
        rule.setPublicationStatus(PublicationStatus.DRAFT);
    }

    private ProductPlacementRule getRule(Long id) {
        return placementRuleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy cấu hình trưng bày: " + id));
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

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
