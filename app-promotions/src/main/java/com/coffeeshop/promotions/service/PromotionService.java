package com.coffeeshop.promotions.service;

import com.coffeeshop.promotions.dto.request.PromotionItemRequest;
import com.coffeeshop.promotions.dto.request.PromotionRequest;
import com.coffeeshop.promotions.dto.response.PromotionAnalyticsResponse;
import com.coffeeshop.promotions.dto.response.PromotionResponse;
import com.coffeeshop.promotions.entity.*;
import com.coffeeshop.promotions.entity.enums.PromotionScope;
import com.coffeeshop.promotions.entity.enums.PromotionStatus;
import com.coffeeshop.promotions.entity.enums.PromotionType;
import com.coffeeshop.promotions.entity.enums.VoucherStatus;
import com.coffeeshop.promotions.repository.CoffeeRepository;
import com.coffeeshop.promotions.repository.OrderRepository;
import com.coffeeshop.promotions.repository.PromotionRepository;
import com.coffeeshop.promotions.repository.VoucherDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final CoffeeRepository coffeeRepository;
    private final OrderRepository orderRepository;
    private final VoucherDetailRepository voucherDetailRepository;

    @Transactional(readOnly = true)
    public List<PromotionResponse> findAll() {
        return promotionRepository.findAll().stream()
                .sorted(
                        Comparator.comparingInt((Promotion p) -> getStatusPriority(p.getStatus()))
                                .thenComparing(Promotion::getStartDate, Comparator.nullsLast(Comparator.reverseOrder()))
                )
                .map(PromotionResponse::new)
                .collect(Collectors.toList());
    }

    private int getStatusPriority(PromotionStatus status) {
        if (status == null) return 5;
        switch (status) {
            case ACTIVE:
                return 1;
            case PAUSED:
                return 2;
            case DRAFT:
                return 3;
            case ENDED:
            default:
                return 4;
        }
    }

    @Transactional(readOnly = true)
    public PromotionResponse findById(Long id) {
        Promotion promotion = findPromotionById(id);
        return new PromotionResponse(promotion);
    }

    @Transactional
    public PromotionResponse create(PromotionRequest request) {

        PromotionStatus defaultStatus = PromotionStatus.DRAFT;
        Promotion promotion = Promotion.builder()
                .name(request.getName())
                .description(request.getDescription())
                .applyScope(request.getApplyScope())
                .discountType(request.getDiscountType())
                .discountPercent(request.getDiscountPercent())
                .minOrderAmount(request.getMinOrderAmount())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(defaultStatus)
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
        promotion.setStatus(request.getStatus() != null ? request.getStatus() : PromotionStatus.DRAFT);

        if (request.getItems() != null) {
            List<Long> requestNoNull = request.getItems().stream()
                    .map(PromotionItemRequest::getId)
                    .filter(itemId -> itemId != null)
                    .collect(Collectors.toList());

            promotion.getPromotionItems().removeIf(item -> !requestNoNull.contains(item.getId()));


            promotionRepository.flush();


            for (PromotionItemRequest itemReq : request.getItems()) {
                Coffee coffee = findCoffeeById(itemReq.getCoffeeId());

                if (itemReq.getId() != null) {

                    PromotionItem existingItem = promotion.getPromotionItems().stream()
                            .filter(i -> i.getId().equals(itemReq.getId()))
                            .findFirst()
                            .orElseThrow(() -> new RuntimeException("PromotionItem not found with id: " + itemReq.getId()));

                    existingItem.setCoffee(coffee);
                    existingItem.setCustomDiscount(itemReq.getCustomDiscount());
                    existingItem.setSalePrice(itemReq.getSalePrice());
                    existingItem.setMaxQuantity(itemReq.getMaxQuantity());
                    if (itemReq.getSoldQuantity() != null) {
                        existingItem.setSoldQuantity(itemReq.getSoldQuantity());
                    }
                } else {

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


    @Transactional(readOnly = true)
    public PromotionAnalyticsResponse getAnalytics(String fromStr, String toStr) {
        LocalDateTime fromDate = (fromStr != null && !fromStr.isEmpty())
                ? LocalDate.parse(fromStr).atStartOfDay()
                : LocalDateTime.now().minusDays(30);
        LocalDateTime toDate = (toStr != null && !toStr.isEmpty())
                ? LocalDate.parse(toStr).atTime(23, 59, 59)
                : LocalDateTime.now();

        List<Promotion> promotions = promotionRepository.findAll();
        List<PromotionStat> promotionStats = promotions.stream().map(promo -> {

            Long orders = orderRepository.countOrdersByPromotion(promo.getId(), fromDate, toDate);
            BigDecimal revenue = orderRepository.sumRevenueByPromotion(promo.getId(), fromDate, toDate);
            BigDecimal discountTotal = orderRepository.sumDiscountByPromotion(promo.getId(), fromDate, toDate);

            orders = (orders != null) ? orders : 0L;
            revenue = (revenue != null) ? revenue : BigDecimal.ZERO;
            discountTotal = (discountTotal != null) ? discountTotal : BigDecimal.ZERO;

            BigDecimal avgOrderValue = orders > 0
                    ? revenue.divide(BigDecimal.valueOf(orders), 2)
                    : BigDecimal.ZERO;

            BigDecimal roi = BigDecimal.ZERO;
            if (discountTotal.compareTo(BigDecimal.ZERO) > 0) {
                roi = revenue.subtract(discountTotal).divide(discountTotal, 2);
            }

            return PromotionStat.builder()
                    .id(promo.getId())
                    .name(promo.getName())
                    .status(promo.getStatus())
                    .orders(orders)
                    .revenue(revenue)
                    .discountTotal(discountTotal)
                    .avgOrderValue(avgOrderValue)
                    .roi(roi)
                    .build();
        }).collect(Collectors.toList());


        long issued = voucherDetailRepository.countByStatus(VoucherStatus.ISSUED);
        long used = voucherDetailRepository.countByStatus(VoucherStatus.USED);
        long expired = voucherDetailRepository.countByStatus(VoucherStatus.EXPIRED);

        double usageRate = issued > 0 ? ((double) used / issued) * 100.0 : 0.0;

        VoucherStat voucherStat = VoucherStat.builder()
                .issued(issued)
                .used(used)
                .expired(expired)
                .usageRate(Math.round(usageRate * 10.0) / 10.0)
                .discountTotal(BigDecimal.ZERO)
                .build();

        BaselineStat baselineStat = BaselineStat.builder()
                .avgOrderValue(new BigDecimal("50000"))
                .ordersPerDay(new BigDecimal("25"))
                .build();

        return PromotionAnalyticsResponse.builder()
                .promotions(promotionStats)
                .vouchers(voucherStat)
                .baseline(baselineStat)
                .build();
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
    public void activePromotion(Long id) {
        Promotion promotion = findPromotionById(id);

        if (promotion.getStatus() != PromotionStatus.DRAFT && promotion.getStatus() != PromotionStatus.PAUSED) {
            throw new RuntimeException("Chỉ có thể kích hoạt chương trình ở trạng thái DRAFT và PAUSED");
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

    @Transactional
    public PromotionResponse clonePromotion(Long id) {
        Promotion promotion = findPromotionById(id);

        String baseName = promotion.getName() + " (Bản sao";

        long count = promotionRepository.countByNameStartingWith(baseName);

        String cloneName = count == 0 ? promotion.getName() + " (Bản sao)" : promotion.getName() + "(Bản sao " + (count + 1) + ")";

        Promotion clonedPromotion = Promotion.builder()
                .name(cloneName)
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

    @Transactional
    public void removePromotion(Long id){
        Promotion promotion = findPromotionById(id);

        if (promotion.getStatus() != PromotionStatus.DRAFT){
            throw new RuntimeException("Chỉ có thể xóa chương trình khuyến mãi đang ở trạng thái DRAFT");
        }

        if (promotion.getPromotionItems() != null) {
            for (PromotionItem item : promotion.getPromotionItems()) {
                item.setDelFlag(true);
            }
        }

        promotionRepository.delete(promotion);
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void autoEndExpiredPromotions() {
        LocalDateTime now = LocalDateTime.now();

        List<Promotion> activeOrPausedPromotions = promotionRepository.findByStatusIn(
                List.of(PromotionStatus.ACTIVE, PromotionStatus.PAUSED)
        );

        for (Promotion promo : activeOrPausedPromotions) {
            if (promo.getEndDate() != null && now.isAfter(promo.getEndDate())) {
                promo.setStatus(PromotionStatus.ENDED);

                if (promo.getApplyScope() == PromotionScope.COFFEE && promo.getPromotionItems() != null) {
                    for (PromotionItem item : promo.getPromotionItems()) {
                        Coffee coffee = item.getCoffee();
                        if (coffee != null) {
                            coffee.resetSalePrice();
                        }
                    }
                }
                promotionRepository.save(promo);
            }
        }
    }

}