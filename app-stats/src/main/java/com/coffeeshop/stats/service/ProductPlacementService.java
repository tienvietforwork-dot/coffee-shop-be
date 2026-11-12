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
import com.coffeeshop.stats.util.ShopTime;
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
        LocalDateTime now = ShopTime.now();
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
        LocalDateTime now = ShopTime.now();
        return placementRuleRepository.findAllByOrderBySectionAscDisplayOrderAscIdAsc()
                .stream()
                .filter(rule -> section == null || rule.getSection() == section)
                .map(rule -> ProductPlacementResponse.from(rule, now))
                .filter(rule -> !visibleOnly || rule.visibleNow())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductPlacementResponse> findPublicRules(ProductPlacementSection section) {
        LocalDateTime now = ShopTime.now();
        return placementRuleRepository.findAllByOrderBySectionAscDisplayOrderAscIdAsc().stream()
                .filter(rule -> section == null || rule.getSection() == section)
                .map(rule -> ProductPlacementResponse.from(rule, now))
                .filter(ProductPlacementResponse::visibleNow)
                .toList();
    }

    @Transactional
    public ProductPlacementResponse create(ProductPlacementRequest request) {
        validateSchedule(request.startsAt(), request.endsAt());
        Category targetCategory = resolveTargetCategory(request.section(), request.targetCategoryId());
        rejectDuplicate(request.coffeeId(), request.section(), targetCategory, null);

        ProductPlacementRule rule = new ProductPlacementRule();
        apply(rule, request, targetCategory);
        return ProductPlacementResponse.from(placementRuleRepository.save(rule), ShopTime.now());
    }

    @Transactional
    public ProductPlacementResponse update(Long id, ProductPlacementRequest request) {
        validateSchedule(request.startsAt(), request.endsAt());
        ProductPlacementRule rule = getRule(id);
        Category targetCategory = resolveTargetCategory(request.section(), request.targetCategoryId());
        rejectDuplicate(request.coffeeId(), request.section(), targetCategory, id);

        if (rule.getPublicationStatus() == PublicationStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cần thu hồi cấu hình đã công bố trước khi sửa");
        }
        apply(rule, request, targetCategory);
        return ProductPlacementResponse.from(rule, ShopTime.now());
    }

    @Transactional
    public ProductPlacementResponse publish(Long id) {
        ProductPlacementRule rule = getRule(id);
        Coffee coffee = rule.getCoffee();
        if (coffee.isAutoSoldOut() || coffee.getStatus() == CoffeeStatus.SOLD_OUT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    coffee.getName() + " đã hết hàng. Hãy bổ sung nguyên liệu hoặc tồn kho rồi thử lại.");
        }
        if (coffee.getStatus() == CoffeeStatus.HIDDEN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    coffee.getName() + " đang bị ẩn. Hãy chuyển sang trạng thái đang bán trước khi công bố.");
        }
        if (coffee.getStatus() == CoffeeStatus.DISCONTINUED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    coffee.getName() + " đã ngừng bán và không thể công bố.");
        }
        if (coffee.getStatus() != CoffeeStatus.AVAILABLE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    coffee.getName() + " chưa ở trạng thái đang bán.");
        }
        if (!rule.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cấu hình trưng bày đang tắt. Hãy bật cấu hình trước khi công bố.");
        }
        if (rule.getSection() == ProductPlacementSection.CATEGORY) {
            Long targetCategoryId = rule.getTargetCategory() == null ? null : rule.getTargetCategory().getId();
            if (targetCategoryId == null || categoryRepository.findById(targetCategoryId).isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Danh mục đích không còn tồn tại. Hãy sửa cấu hình và chọn danh mục khác.");
            }
        }
        if (rule.getEndsAt() != null && !rule.getEndsAt().isAfter(ShopTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không công bố cấu hình đã hết hạn");
        }
        rule.setPublicationStatus(PublicationStatus.PUBLISHED);
        return ProductPlacementResponse.from(rule, ShopTime.now());
    }

    @Transactional
    public ProductPlacementResponse unpublish(Long id) {
        ProductPlacementRule rule = getRule(id);
        rule.setPublicationStatus(PublicationStatus.DRAFT);
        return ProductPlacementResponse.from(rule, ShopTime.now());
    }

    @Transactional
    public void delete(Long id) {
        getRule(id).softDelete();
    }

    @Transactional
    public CoffeePlacementResponse updateCoffeeVisibility(Long coffeeId, CoffeeVisibilityRequest request) {
        Coffee coffee = getCoffee(coffeeId);
        if (coffee.getStatus() == CoffeeStatus.DISCONTINUED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    coffee.getName() + " đã ngừng bán vĩnh viễn, không thể đổi trạng thái tại đây.");
        }
        CoffeeStatus status = request.status();
        if (status == CoffeeStatus.DISCONTINUED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Product Placement chỉ dùng để ẩn/hiện sản phẩm, không ngừng bán vĩnh viễn");
        }
        if (status == CoffeeStatus.AVAILABLE && coffee.isAutoSoldOut()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    coffee.getName() + " đã hết nguyên liệu nên chưa thể chuyển sang đang bán.");
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

    private void apply(ProductPlacementRule rule, ProductPlacementRequest request, Category targetCategory) {
        Coffee coffee = getCoffee(request.coffeeId());
        if (coffee.getStatus() == CoffeeStatus.DISCONTINUED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    coffee.getName() + " đã ngừng bán và không thể đưa vào khu vực trưng bày.");
        }
        boolean coffeeChanged = rule.getId() == null || rule.getCoffee() == null
                || !rule.getCoffee().getId().equals(coffee.getId());
        if (coffeeChanged && (coffee.getStatus() != CoffeeStatus.AVAILABLE || coffee.isAutoSoldOut())) {
            throw unavailableCoffee(coffee);
        }
        if (request.publicationStatus() != null && request.publicationStatus() != PublicationStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dùng API publish để công bố cấu hình");
        }
        rule.setCoffee(coffee);
        rule.setTargetCategory(targetCategory);
        rule.setSection(request.section());
        rule.setTitle(blankToNull(request.title()));
        rule.setDisplayOrder(request.displayOrder() == null ? 0 : request.displayOrder());
        rule.setStartsAt(request.startsAt());
        rule.setEndsAt(request.endsAt());
        rule.setActive(request.active() == null || request.active());
        rule.setPublicationStatus(PublicationStatus.DRAFT);
    }

    private Category resolveTargetCategory(ProductPlacementSection section, Long targetCategoryId) {
        if (section == ProductPlacementSection.CATEGORY) {
            if (targetCategoryId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Chọn danh mục cần đặt sản phẩm ở đầu danh mục.");
            }
            return categoryRepository.findById(targetCategoryId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Không tìm thấy danh mục trưng bày: " + targetCategoryId));
        }
        if (targetCategoryId != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Chỉ khu vực Đầu danh mục mới được chọn danh mục đích.");
        }
        return null;
    }

    private void rejectDuplicate(Long coffeeId, ProductPlacementSection section, Category targetCategory, Long exceptId) {
        boolean duplicate = targetCategory == null
                ? (exceptId == null
                    ? placementRuleRepository.existsByCoffee_IdAndSectionAndTargetCategoryIsNull(coffeeId, section)
                    : placementRuleRepository.existsByCoffee_IdAndSectionAndTargetCategoryIsNullAndIdNot(coffeeId, section, exceptId))
                : (exceptId == null
                    ? placementRuleRepository.existsByCoffee_IdAndSectionAndTargetCategory_Id(coffeeId, section, targetCategory.getId())
                    : placementRuleRepository.existsByCoffee_IdAndSectionAndTargetCategory_IdAndIdNot(coffeeId, section, targetCategory.getId(), exceptId));
        if (duplicate) {
            String location = targetCategory == null ? section.name() : "đầu danh mục " + targetCategory.getName();
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Sản phẩm đã có cấu hình trong khu vực " + location + ". Hãy sửa cấu hình hiện có.");
        }
    }

    private ResponseStatusException unavailableCoffee(Coffee coffee) {
        if (coffee.isAutoSoldOut() || coffee.getStatus() == CoffeeStatus.SOLD_OUT) {
            return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    coffee.getName() + " đã hết hàng. Chỉ có thể chọn món đang bán và còn hàng.");
        }
        if (coffee.getStatus() == CoffeeStatus.HIDDEN) {
            return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    coffee.getName() + " đang bị ẩn. Hãy chuyển sang Đang bán trước khi chọn.");
        }
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                coffee.getName() + " hiện không thể trưng bày.");
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
        if (endsAt != null && !endsAt.isAfter(ShopTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Thời gian kết thúc phải ở tương lai");
        }
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
