package com.coffeeshop.service;

import com.coffeeshop.entity.DeliverySubscription;
import com.coffeeshop.entity.Order;
import com.coffeeshop.entity.enums.OrderStatus;
import com.coffeeshop.entity.enums.OrderType;
import com.coffeeshop.entity.enums.SubscriptionStatus;
import com.coffeeshop.repository.DeliverySubscriptionRepository;
import com.coffeeshop.repository.OrderRepository;
import com.coffeeshop.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Lịch hẹn giao / nhận và thứ tự pha chế: mỗi đơn mang đi / giao đang xử lý có giờ cần giao tới (giờ hẹn, hoặc càng sớm
 * càng tốt), thời gian pha (công thức) và thời gian chạy xe (km ÷ tốc độ) → giờ phải bắt đầu pha. Đơn nào phải pha sớm
 * nhất xếp trước. Lần giao sắp tới của gói định kỳ (chưa tự tạo đơn) cũng hiện để quầy chuẩn bị.
 */
@Service
@RequiredArgsConstructor
public class DeliveryScheduleService {

    private static final Set<OrderStatus> ACTIVE = EnumSet.of(OrderStatus.PENDING, OrderStatus.CONFIRMED,
            OrderStatus.PREPARING, OrderStatus.READY, OrderStatus.DELIVERING);
    /** đã bắt đầu pha: không còn "cần pha" */
    private static final Set<OrderStatus> STARTED = EnumSet.of(OrderStatus.PREPARING, OrderStatus.READY, OrderStatus.DELIVERING);

    private final OrderRepository orderRepository;
    private final RecipeRepository recipeRepository;
    private final DeliverySubscriptionRepository subscriptionRepository;
    private final DeliveryFeeService deliveryFeeService;

    /** phút dự phòng (đóng gói, bàn giao người giao) */
    @Value("${app.schedule.buffer-minutes:5}")
    private int bufferMinutes;
    /** không đo được quãng đường: coi như chạy xe bấy nhiêu phút */
    @Value("${app.schedule.default-travel-minutes:20}")
    private int defaultTravelMinutes;
    /** gói định kỳ tự tạo đơn trước giờ giao bấy nhiêu phút (như SubscriptionService) */
    @Value("${app.subscription.lead-minutes:60}")
    private int leadMinutes;

    /**
     * Một dòng lịch: {@code orderId} null = lần giao dự kiến của gói định kỳ, đơn sẽ tự tạo lúc {@code autoCreateAt}.
     * {@code urgency}: LATE (đã quá giờ phải pha mà chưa pha), NOW (trong 10 phút tới), SOON (trong 30 phút), LATER,
     * STARTED (đang pha / xong / đang giao).
     */
    public record Row(Long orderId, String orderCode, OrderStatus status, OrderType orderType, String customerName,
                      String phone, String address, String zone, String items, int cups, boolean scheduled,
                      LocalDateTime deliverAt, int prepMinutes, int travelMinutes, LocalDateTime brewStartAt,
                      String urgency, String shipperName, String subscriptionCode, LocalDateTime autoCreateAt, String note) {
    }

    @Transactional(readOnly = true)
    public List<Row> schedule(LocalDate date) {
        LocalDateTime now = LocalDateTime.now(DeliveryFeeService.SHOP_ZONE);
        LocalDate day = date == null ? now.toLocalDate() : date;
        Map<Long, Integer> brew = brewMinutes();
        BigDecimal speed = deliveryFeeService.settings().speedKmh();
        boolean today = day.equals(now.toLocalDate());

        List<Row> rows = new ArrayList<>();
        Set<Long> subsWithOrder = new HashSet<>();
        for (Order o : orderRepository.findSchedule(ACTIVE, day.atStartOfDay(), day.plusDays(1).atStartOfDay())) {
            if (o.getPickupTime() == null && !today) continue; // đơn làm ngay chỉ hiện trong lịch hôm nay
            if (o.getSubscriptionId() != null) subsWithOrder.add(o.getSubscriptionId());
            int prep = o.getItems().stream().mapToInt(i -> brew.getOrDefault(i.getCoffee().getId(), 0) * i.getQuantity()).sum();
            var sh = o.getShipment();
            int travel = o.getOrderType() != OrderType.DELIVERY ? 0 : travelMinutes(sh == null ? null : sh.getDistanceKm(), speed);
            boolean scheduled = o.getPickupTime() != null;
            // không hẹn giờ: pha ngay khi nhận đơn
            LocalDateTime deliverAt = scheduled ? o.getPickupTime() : o.getOrderedAt().plusMinutes(prep + travel + bufferMinutes);
            LocalDateTime brewAt = scheduled ? deliverAt.minusMinutes(prep + travel + bufferMinutes) : o.getOrderedAt();
            rows.add(new Row(o.getId(), o.getOrderCode(), o.getStatus(), o.getOrderType(),
                    o.getCustomer() == null ? null : o.getCustomer().getFullName(),
                    sh != null ? sh.getAddress().getRecipientPhone() : o.getCustomer() == null ? null : o.getCustomer().getPhone(),
                    sh == null ? null : sh.getAddress().getAddress(), sh == null ? null : sh.getZoneName(),
                    o.getItems().stream().map(i -> i.getQuantity() + "× " + i.getCoffee().getName()).collect(Collectors.joining(", ")),
                    o.getItems().stream().mapToInt(i -> i.getQuantity()).sum(), scheduled, deliverAt, prep, travel, brewAt,
                    STARTED.contains(o.getStatus()) ? "STARTED" : urgency(now, brewAt),
                    sh == null || sh.getShipper() == null ? null : sh.getShipper().getFullName(), null, null, o.getNote()));
        }
        // lần giao của gói định kỳ chưa có đơn
        for (DeliverySubscription s : subscriptionRepository.findByStatus(SubscriptionStatus.ACTIVE)) {
            if (!s.runsOn(day) || subsWithOrder.contains(s.getId()) || day.equals(s.getLastRunDate())) continue;
            LocalDateTime deliverAt = day.atTime(s.getDeliveryTime());
            if (deliverAt.isBefore(now)) continue;
            int prep = s.getItems().stream().mapToInt(i -> brew.getOrDefault(i.getCoffee().getId(), 0) * i.getQuantity()).sum();
            int travel = defaultTravelMinutes;
            LocalDateTime brewAt = deliverAt.minusMinutes(prep + travel + bufferMinutes);
            rows.add(new Row(null, null, null, OrderType.DELIVERY, s.getCustomer().getFullName(), s.getAddress().getRecipientPhone(),
                    s.getAddress().getAddress(), null,
                    s.getItems().stream().map(i -> i.getQuantity() + "× " + i.getCoffee().getName()).collect(Collectors.joining(", ")),
                    s.getItems().stream().mapToInt(i -> i.getQuantity()).sum(), true, deliverAt, prep, travel, brewAt,
                    urgency(now, brewAt), null, s.getCode(), deliverAt.minusMinutes(leadMinutes), s.getNote()));
        }
        // đã bắt đầu pha xuống cuối, còn lại: phải pha sớm nhất lên đầu
        rows.sort(Comparator.comparing((Row r) -> "STARTED".equals(r.urgency())).thenComparing(Row::brewStartAt));
        return rows;
    }

    /**
     * Giờ phải bắt đầu pha của từng đơn (id → giờ): đơn hẹn giờ = giờ hẹn − thời gian pha − chạy xe − dự phòng; đơn
     * làm ngay = lúc đặt. Bảng điều phối dùng để tách cột "Hẹn giao" / "Chờ pha chế" và xếp thứ tự ưu tiên.
     */
    @Transactional(readOnly = true)
    public Map<Long, LocalDateTime> brewStartTimes(List<Order> orders) {
        Map<Long, LocalDateTime> out = new HashMap<>();
        if (orders.isEmpty()) return out;
        Map<Long, Integer> brew = null;
        BigDecimal speed = null;
        for (Order o : orders) {
            if (o.getPickupTime() == null) {
                out.put(o.getId(), o.getOrderedAt());
                continue;
            }
            if (brew == null) {
                brew = brewMinutes();
                speed = deliveryFeeService.settings().speedKmh();
            }
            Map<Long, Integer> b = brew;
            int prep = o.getItems().stream().mapToInt(i -> b.getOrDefault(i.getCoffee().getId(), 0) * i.getQuantity()).sum();
            var sh = o.getShipment();
            int travel = o.getOrderType() != OrderType.DELIVERY ? 0 : travelMinutes(sh == null ? null : sh.getDistanceKm(), speed);
            out.put(o.getId(), o.getPickupTime().minusMinutes(prep + travel + bufferMinutes));
        }
        return out;
    }

    private Map<Long, Integer> brewMinutes() {
        Map<Long, Integer> brew = new HashMap<>();
        recipeRepository.findAllActive().forEach(r -> {
            if (r.getBrewTimeMin() != null) brew.putIfAbsent(r.getCoffee().getId(), r.getBrewTimeMin());
        });
        return brew;
    }

    private int travelMinutes(BigDecimal km, BigDecimal speedKmh) {
        if (km == null || speedKmh == null || speedKmh.signum() <= 0) return defaultTravelMinutes;
        return Math.max(1, km.multiply(BigDecimal.valueOf(60)).divide(speedKmh, 0, RoundingMode.CEILING).intValue());
    }

    private static String urgency(LocalDateTime now, LocalDateTime brewAt) {
        long minutes = Duration.between(now, brewAt).toMinutes();
        if (minutes < 0) return "LATE";
        if (minutes <= 10) return "NOW";
        if (minutes <= 30) return "SOON";
        return "LATER";
    }
}
