package com.coffeeshop.dto.response;

import com.coffeeshop.entity.DiningTable;
import com.coffeeshop.entity.Order;
import com.coffeeshop.entity.enums.OrderStatus;
import com.coffeeshop.entity.enums.PaymentStatus;
import com.coffeeshop.entity.enums.TableStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Một ô trên sơ đồ bàn (order tại quầy). {@code occupied}: bàn còn đơn đang mở (đang làm hoặc chưa thu tiền);
 * {@code total} tổng các đơn đang mở, {@code unpaid} phần chưa thu; {@code since} giờ khách gọi đơn đầu tiên;
 * {@code waiting} số đơn khách tự gọi qua QR chưa được nhận pha.
 */
public record TableBoardResponse(Long id, String tableNo, String floor, String area, Integer capacity,
                                 TableStatus status, boolean occupied, int orderCount, BigDecimal total,
                                 BigDecimal unpaid, LocalDateTime since, int waiting) {

    public static TableBoardResponse of(DiningTable t, List<Order> open) {
        BigDecimal total = open.stream().map(Order::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal unpaid = open.stream().flatMap(o -> o.getPayments().stream())
                .filter(p -> p.getStatus() == PaymentStatus.PENDING)
                .map(p -> p.getAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDateTime since = open.stream().map(Order::getOrderedAt).min(LocalDateTime::compareTo).orElse(null);
        int waiting = (int) open.stream()
                .filter(o -> o.getStatus() == OrderStatus.PENDING || o.getStatus() == OrderStatus.CONFIRMED).count();
        boolean occupied = !open.isEmpty() || t.getStatus() == TableStatus.OCCUPIED;
        return new TableBoardResponse(t.getId(), t.getTableNo(), t.getFloor(), t.getArea(), t.getCapacity(),
                t.getStatus(), occupied, open.size(), total, unpaid, since, waiting);
    }
}
