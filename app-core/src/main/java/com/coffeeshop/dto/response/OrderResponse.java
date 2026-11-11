package com.coffeeshop.dto.response;

import com.coffeeshop.entity.Order;
import com.coffeeshop.entity.enums.OrderChannel;
import com.coffeeshop.entity.enums.OrderStatus;
import com.coffeeshop.entity.enums.OrderType;
import com.coffeeshop.entity.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Giảm giá của đơn tách theo nguồn: {@code itemDiscount} = đợt giảm giá theo món (giá niêm yết − giá bán, đã trừ sẵn
 * trong {@code subtotal}); {@code promotionDiscount} = đợt giảm giá theo đơn; {@code voucherDiscount} = voucher
 * {@code voucherCode}. {@code discountAmount} = promotionDiscount + voucherDiscount (trừ trên subtotal).
 */
public record OrderResponse(Long id, String orderCode, OrderStatus status, OrderType orderType, OrderChannel channel,
                            Long customerId, String customerName, String customerPhone,
                            Long tableId, String tableNo, Long staffId, String staffName, String promotionName,
                            BigDecimal subtotal, BigDecimal itemDiscount, BigDecimal promotionDiscount,
                            String voucherCode, BigDecimal voucherDiscount, BigDecimal discountAmount,
                            BigDecimal shippingFee, BigDecimal totalAmount, String note, String cancelReason,
                            LocalDateTime pickupTime, LocalDateTime orderedAt, boolean paid, List<Item> items,
                            List<PaymentResponse> payments, ShipmentResponse shipment) {

    /** {@code listPrice}: giá niêm yết lúc đặt; lớn hơn {@code unitPrice} khi món được giảm giá. */
    public record Item(Long id, Long coffeeId, String coffeeCode, String coffeeName, String imageUrl, Integer quantity,
                       BigDecimal listPrice, BigDecimal unitPrice, BigDecimal lineTotal, String note) {
    }

    public static OrderResponse from(Order o) {
        var customer = o.getCustomer();
        var table = o.getTable();
        var staff = o.getStaff();
        var shipment = o.getShipment();
        boolean paid = !o.getPayments().isEmpty()
                && o.getPayments().stream().allMatch(p -> p.getStatus() == PaymentStatus.PAID
                || p.getStatus() == PaymentStatus.REFUNDED || p.getStatus() == PaymentStatus.REFUND_PENDING);
        List<Item> items = o.getItems().stream().map(i -> new Item(i.getId(), i.getCoffee().getId(),
                i.getCoffee().getCode(), i.getCoffee().getName(), i.getCoffee().imageSrc(), i.getQuantity(),
                i.getListPrice() == null ? i.getUnitPrice() : i.getListPrice(), i.getUnitPrice(), i.getLineTotal(),
                i.getNote())).toList();
        BigDecimal itemDiscount = items.stream()
                .map(i -> i.listPrice().subtract(i.unitPrice()).max(BigDecimal.ZERO).multiply(BigDecimal.valueOf(i.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal voucherDiscount = o.getVoucherDiscount() == null ? BigDecimal.ZERO : o.getVoucherDiscount();
        return new OrderResponse(o.getId(), o.getOrderCode(), o.getStatus(), o.getOrderType(), o.getChannel(),
                customer == null ? null : customer.getId(),
                customer == null ? null : customer.getFullName(),
                customer == null ? null : customer.getPhone(),
                table == null ? null : table.getId(),
                table == null ? null : table.getTableNo(),
                staff == null ? null : staff.getId(),
                staff == null ? null : staff.getFullName(),
                o.getPromotion() == null ? null : o.getPromotion().getName(),
                o.getSubtotal(), itemDiscount, o.getDiscountAmount().subtract(voucherDiscount).max(BigDecimal.ZERO),
                o.getVoucherCode(), voucherDiscount, o.getDiscountAmount(),
                shipment == null ? BigDecimal.ZERO : shipment.getShippingFee(),
                o.getTotalAmount(), o.getNote(), o.getCancelReason(), o.getPickupTime(), o.getOrderedAt(), paid,
                items,
                o.getPayments().stream().map(PaymentResponse::from).toList(),
                shipment == null ? null : ShipmentResponse.from(shipment));
    }
}
