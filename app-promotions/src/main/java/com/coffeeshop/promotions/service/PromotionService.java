package com.coffeeshop.promotions.service;

import com.coffeeshop.promotions.dto.request.PromotionItemRequest;
import com.coffeeshop.promotions.dto.request.PromotionRequest;
import com.coffeeshop.promotions.dto.response.PromotionResponse;
import com.coffeeshop.promotions.entity.Coffee;
import com.coffeeshop.promotions.entity.Promotion;
import com.coffeeshop.promotions.entity.PromotionItem;
import com.coffeeshop.promotions.entity.enums.PromotionScope;
import com.coffeeshop.promotions.entity.enums.PromotionStatus;
import com.coffeeshop.promotions.entity.enums.PromotionType;
import com.coffeeshop.promotions.repository.CoffeeRepository;
import com.coffeeshop.promotions.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final CoffeeRepository coffeeRepository;

    @Transactional(readOnly = true)
    public List<PromotionResponse> findAll() {
        return promotionRepository.findAll().stream()
                .map(PromotionResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PromotionResponse findById(Long id) {
        Promotion promotion = findPromotionById(id);
        return new PromotionResponse(promotion);
    }

    @Transactional
    public PromotionResponse create(PromotionRequest request) {
        Promotion promotion = Promotion.builder()
                .name(request.getName())
                .description(request.getDescription())
                .applyScope(request.getApplyScope())
                .discountType(request.getDiscountType())
                .discountPercent(request.getDiscountPercent())
                .minOrderAmount(request.getMinOrderAmount())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(request.getStatus())
                .build();

        if (request.getItems() != null) {
            for (PromotionItemRequest itemReq : request.getItems()) {
                Coffee coffee = findCoffeeById(itemReq.getCoffeeId());

                PromotionItem item = PromotionItem.builder()
                        .coffee(coffee)
                        .customDiscount(itemReq.getCustomDiscount())
                        .salePrice(itemReq.getSalePrice())
                        .maxQuantity(itemReq.getMaxQuantity())
                        .soldQuantity(itemReq.getSoldQuantity() != null ? itemReq.getSoldQuantity() : 0)
                        .build();

                promotion.addPromotionItem(item);
            }
        }

        Promotion saved = promotionRepository.save(promotion);
        return new PromotionResponse(saved);
    }

    @Transactional
    public PromotionResponse update(Long id, PromotionRequest request) {
        Promotion promotion = findPromotionById(id);

        promotion.setName(request.getName());
        promotion.setDescription(request.getDescription());
        promotion.setApplyScope(request.getApplyScope());
        promotion.setDiscountType(request.getDiscountType());
        promotion.setDiscountPercent(request.getDiscountPercent());
        promotion.setMinOrderAmount(request.getMinOrderAmount());
        promotion.setStartDate(request.getStartDate());
        promotion.setEndDate(request.getEndDate());
        promotion.setStatus(request.getStatus());

        if (request.getItems() != null) {
            List<Long> itemNoNull = request.getItems().stream()
                    .map(PromotionItemRequest::getId)
                    .filter(itemId -> itemId != null)
                    .collect(Collectors.toList());

            promotion.getPromotionItems().removeIf(item -> !itemNoNull.contains(item.getId()));

            for (PromotionItemRequest itemReq : request.getItems()) {
                if (itemReq.getId() != null) {
                    PromotionItem updateItem = promotion.getPromotionItems().stream()
                            .filter(i -> i.getId().equals(itemReq.getId()))
                            .findFirst()
                            .orElseThrow(() -> new RuntimeException("PromotionItem not found with id: " + itemReq.getId()));

                    Coffee coffee = findCoffeeById(itemReq.getCoffeeId());
                    updateItem.setCoffee(coffee);
                    updateItem.setCustomDiscount(itemReq.getCustomDiscount());
                    updateItem.setSalePrice(itemReq.getSalePrice());
                    updateItem.setMaxQuantity(itemReq.getMaxQuantity());
                    if (itemReq.getSoldQuantity() != null) {
                        updateItem.setSoldQuantity(itemReq.getSoldQuantity());
                    }
                } else {
                    Coffee coffee = findCoffeeById(itemReq.getCoffeeId());

                    PromotionItem newItem = PromotionItem.builder()
                            .coffee(coffee)
                            .customDiscount(itemReq.getCustomDiscount())
                            .salePrice(itemReq.getSalePrice())
                            .maxQuantity(itemReq.getMaxQuantity())
                            .soldQuantity(itemReq.getSoldQuantity() != null ? itemReq.getSoldQuantity() : 0)
                            .build();

                    promotion.addPromotionItem(newItem);
                }
            }
        } else {
            promotion.getPromotionItems().clear();
        }

        Promotion updated = promotionRepository.save(promotion);
        return new PromotionResponse(updated);
    }

    @Transactional
    public void delete(Long id) {
        Promotion promotion = findPromotionById(id);
        for (PromotionItem item : promotion.getPromotionItems()) {
            item.setDelFlag(true);
        }
        promotionRepository.delete(promotion);
    }

    private Promotion findPromotionById(Long id) {
        return promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Promotion not found with id: " + id));
    }

    private Coffee findCoffeeById(Long coffeeId) {
        return coffeeRepository.findById(coffeeId)
                .orElseThrow(() -> new RuntimeException("Coffee not found with id: " + coffeeId));
    }


    @Transactional
    public void tinhToanGiaTri(Promotion promotion){

        PromotionType type = promotion.getDiscountType();

        for (PromotionItem item : promotion.getPromotionItems()){

            Coffee coffee = item.getCoffee();
            if (coffee == null || coffee.getPrice() == null){
                continue;
            }

            BigDecimal giaGoc = coffee.getPrice();
            BigDecimal giaBan = giaGoc;
            BigDecimal discountValue = item.getCustomDiscount();
            BigDecimal salePercentage = BigDecimal.ZERO;

            if (discountValue == null && type != PromotionType.SPECIAL) {
                continue;
            }

            switch (type) {
                case PERCENTAGE:
                    salePercentage = discountValue;
                    BigDecimal percentFactor = discountValue.multiply(new BigDecimal("0.01"));
                    giaBan = giaGoc.multiply(BigDecimal.ONE.subtract(percentFactor));
                    break;

                case FIXED_AMOUNT:
                    giaBan = giaGoc.subtract(discountValue);
                    salePercentage = BigDecimal.ZERO;
                    break;

                case SPECIAL:
                    BigDecimal specialPrice = item.getSalePrice() != null ? item.getSalePrice() : discountValue;
                    if (specialPrice != null) {
                        giaBan = specialPrice;
                    }
                    salePercentage = BigDecimal.ZERO;
                    break;
            }

            if (giaBan.compareTo(BigDecimal.ZERO) < 0) {
                giaBan = BigDecimal.ZERO;
            }
            if (giaBan.compareTo(giaGoc) > 0) {
                giaBan = giaGoc;
            }

            coffee.updatePricing(giaBan, salePercentage);
        }
    }

    @Transactional
    public void activatePromotion(Long id) {
        Promotion promotion = findPromotionById(id);

        if (promotion.getStatus() != PromotionStatus.DRAFT) {
            throw new RuntimeException("Chỉ có thể kích hoạt chương trình ở trạng thái DRAFT");
        }

        LocalDateTime now = LocalDateTime.now();
        if (promotion.getStartDate() == null || promotion.getEndDate() == null) {
            throw new RuntimeException("Chưa thiết lập thời gian bắt đầu hoặc kết thúc cho chương trình");
        }

        if (now.isBefore(promotion.getStartDate()) || now.isAfter(promotion.getEndDate())) {
            throw new RuntimeException("Không thể kích hoạt: Chương trình không nằm trong thời gian hiệu lực hiện tại");
        }

        promotion.setStatus(PromotionStatus.ACTIVE);

        if (promotion.getApplyScope() == PromotionScope.COFFEE) {
            tinhToanGiaTri(promotion);
        }

        promotionRepository.save(promotion);
    }

    @Transactional
    public void pausePromotion(Long id) {
        Promotion promotion = findPromotionById(id);

        if (promotion.getStatus() != PromotionStatus.ACTIVE) {
            throw new RuntimeException("Chỉ có thể tạm ngừng chương trình khuyến mãi đang ở trạng thái ACTIVE");
        }

        LocalDateTime now = LocalDateTime.now();
        if (promotion.getStartDate() == null || promotion.getEndDate() == null) {
            throw new RuntimeException("Chưa thiết lập thời gian bắt đầu hoặc kết thúc cho chương trình");
        }

        if (now.isBefore(promotion.getStartDate()) || now.isAfter(promotion.getEndDate())) {
            throw new RuntimeException("Không thể tạm ngừng: Chương trình không nằm trong khoảng thời gian hiệu lực (startDate - endDate)");
        }

        promotion.setStatus(PromotionStatus.PAUSED);

        if (promotion.getApplyScope() == PromotionScope.COFFEE && promotion.getPromotionItems() != null) {
            for (PromotionItem item : promotion.getPromotionItems()) {
                Coffee coffee = item.getCoffee();
                if (coffee != null) {
                    coffee.resetSalePrice();
                    coffeeRepository.save(coffee);
                }
            }
        }

        promotionRepository.save(promotion);
    }

    @Transactional
    public void resumePromotion(Long id) {
        Promotion promotion = findPromotionById(id);

        if (promotion.getStatus() != PromotionStatus.PAUSED) {
            throw new RuntimeException("Chỉ có thể tiếp tục chương trình khuyến mãi đang ở trạng thái PAUSED");
        }

        LocalDateTime now = LocalDateTime.now();
        if (promotion.getStartDate() == null || promotion.getEndDate() == null) {
            throw new RuntimeException("Chưa thiết lập thời gian bắt đầu hoặc kết thúc cho chương trình");
        }

        if (now.isBefore(promotion.getStartDate()) || now.isAfter(promotion.getEndDate())) {
            throw new RuntimeException("Không thể tiếp tục: Chương trình đã quá hạn hoặc chưa đến thời gian hiệu lực (startDate - endDate)");
        }

        promotion.setStatus(PromotionStatus.ACTIVE);

        if (promotion.getApplyScope() == PromotionScope.COFFEE) {
            tinhToanGiaTri(promotion);
        }

        promotionRepository.save(promotion);
    }


    @Transactional
    public void endPromotion(Long id) {
        Promotion promotion = findPromotionById(id);

        if (promotion.getStatus() != PromotionStatus.ACTIVE && promotion.getStatus() != PromotionStatus.PAUSED) {
            throw new RuntimeException("Chỉ có thể kết thúc chương trình khuyến mãi đang ở trạng thái ACTIVE hoặc PAUSED");
        }

        LocalDateTime now = LocalDateTime.now();
        if (promotion.getStartDate() == null || promotion.getEndDate() == null) {
            throw new RuntimeException("Chưa thiết lập thời gian bắt đầu hoặc kết thúc cho chương trình");
        }

        promotion.setStatus(PromotionStatus.ENDED);

        if (promotion.getApplyScope() == PromotionScope.COFFEE && promotion.getPromotionItems() != null) {
            for (PromotionItem item : promotion.getPromotionItems()) {
                Coffee coffee = item.getCoffee();
                if (coffee != null) {
                    coffee.resetSalePrice();
                }
            }
        }

        promotionRepository.save(promotion);
    }

    /**
     * Nhân bản chương trình khuyến mãi dựa theo ID
     */
    @Transactional
    public PromotionResponse clonePromotion(Long id) {
        Promotion promotion = findPromotionById(id);

        Promotion clonedPromotion = Promotion.builder()
                .name(promotion.getName() + " (Bản sao)")
                .description(promotion.getDescription())
                .applyScope(promotion.getApplyScope())
                .discountType(promotion.getDiscountType())
                .discountPercent(promotion.getDiscountPercent())
                .minOrderAmount(promotion.getMinOrderAmount())
                .startDate(promotion.getStartDate())
                .endDate(promotion.getEndDate())
                .status(PromotionStatus.DRAFT)
                .build();

        if (promotion.getPromotionItems() != null) {
            for (PromotionItem originalItem : promotion.getPromotionItems()) {
                PromotionItem clonedItem = PromotionItem.builder()
                        .coffee(originalItem.getCoffee())
                        .customDiscount(originalItem.getCustomDiscount())
                        .salePrice(originalItem.getSalePrice())
                        .maxQuantity(originalItem.getMaxQuantity())
                        .soldQuantity(0)
                        .build();

                clonedPromotion.addPromotionItem(clonedItem);
            }
        }

        Promotion savedPromotion = promotionRepository.save(clonedPromotion);
        return new PromotionResponse(savedPromotion);
    }

}