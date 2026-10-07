package com.coffeeshop.service;

import com.coffeeshop.dto.request.*;
import com.coffeeshop.dto.response.OrderResponse;
import com.coffeeshop.dto.response.PaymentResponse;
import com.coffeeshop.dto.response.PriceQuoteResponse;
import com.coffeeshop.entity.*;
import com.coffeeshop.entity.enums.*;
import com.coffeeshop.exception.BadRequestException;
import com.coffeeshop.exception.ResourceNotFoundException;
import com.coffeeshop.repository.*;
import com.coffeeshop.security.CurrentUser;
import com.coffeeshop.websocket.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Order lifecycle (BPMN-01 / BPMN-02):
 * <pre>
 * PENDING ─confirm→ CONFIRMED ─prepare→ PREPARING ─ready→ READY ─complete→ COMPLETED
 *    │                  │                                    └─dispatch→ DELIVERING ─complete→ COMPLETED
 *    └─reject→ REJECTED └─cancel→ CANCELLED (also from PENDING)
 * </pre>
 * Materials are deducted when the order starts PREPARING; loyalty points are added on COMPLETED.
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Set<OrderStatus> ACTIVE = EnumSet.of(OrderStatus.PENDING, OrderStatus.CONFIRMED,
            OrderStatus.PREPARING, OrderStatus.READY, OrderStatus.DELIVERING);
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ShipmentRepository shipmentRepository;
    private final VoucherRepository voucherRepository;
    private final LoyaltyPointHistoryRepository loyaltyRepository;
    private final CartService cartService;
    private final CoffeeService coffeeService;
    private final CustomerService customerService;
    private final DiningTableService tableService;
    private final StaffService staffService;
    private final PricingService pricingService;
    private final InventoryService inventoryService;
    private final StockService stockService;
    private final NotificationService notificationService;

    /** Loyalty: 1 point per this many VND paid. */
    @Value("${app.loyalty.vnd-per-point:10000}")
    private BigDecimal vndPerPoint;

    // ---- queries ---------------------------------------------------------

    @Transactional(readOnly = true)
    public List<OrderResponse> search(OrderStatus status, OrderChannel channel, LocalDate from, LocalDate to) {
        LocalDateTime start = (from == null ? LocalDate.now().minusDays(30) : from).atStartOfDay();
        LocalDateTime end = (to == null ? LocalDate.now() : to).plusDays(1).atStartOfDay();
        return orderRepository.search(status, channel, start, end).stream().map(OrderResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(Long id) {
        return OrderResponse.from(get(id));
    }

    @Transactional(readOnly = true)
    public OrderResponse findByCode(String code) {
        return OrderResponse.from(getByCode(code));
    }

    // ---- pricing preview -------------------------------------------------

    @Transactional(readOnly = true)
    public PriceQuoteResponse previewCart(String sessionCode, OrderType orderType, String customerPhone,
                                          String voucherCode) {
        Cart cart = cartService.getOpen(sessionCode);
        var lines = cart.getItems().stream()
                .map(i -> new PricingService.InputLine(i.getCoffee(), i.getQuantity(), i.getNote())).toList();
        return pricingService.quote(lines, orderType, phoneOfCurrentCustomer(customerPhone), voucherCode,
                LocalDateTime.now()).toResponse();
    }

    @Transactional(readOnly = true)
    public PriceQuoteResponse preview(PricePreviewRequest request) {
        return pricingService.quote(toLines(request.items()), request.orderType(), request.customerPhone(),
                request.voucherCode(), LocalDateTime.now()).toResponse();
    }

    // ---- creation --------------------------------------------------------

    /** Customer checkout from a QR (table) or online cart. */
    @Transactional
    public OrderResponse checkout(CheckoutRequest request) {
        Cart cart = cartService.getOpen(request.sessionCode());
        if (cart.getItems().isEmpty()) throw new BadRequestException("Giỏ hàng đang trống");
        if (request.orderType() == OrderType.DINE_IN && cart.getTable() == null) {
            throw new BadRequestException("Đơn tại bàn cần quét mã QR trên bàn");
        }
        var lines = cart.getItems().stream()
                .map(i -> new PricingService.InputLine(i.getCoffee(), i.getQuantity(), i.getNote())).toList();
        Draft draft = new Draft(lines, request.orderType(), cart.getTable() != null ? OrderChannel.QR : OrderChannel.ONLINE,
                request.orderType() == OrderType.DINE_IN ? cart.getTable() : null, cart, request.customerPhone(),
                request.customerName(), request.addressId(), request.delivery(), request.pickupTime(),
                request.paymentMethod(), false, request.voucherCode(), request.note());
        Order order = place(draft);
        cart.setStatus(CartStatus.ORDERED);
        return OrderResponse.from(order);
    }

    /** Staff takes an order at the counter: confirmed immediately. */
    @Transactional
    public OrderResponse createAtCounter(CounterOrderRequest request) {
        DiningTable table = request.tableId() == null ? null : tableService.get(request.tableId());
        if (request.orderType() == OrderType.DINE_IN && table == null) {
            throw new BadRequestException("Đơn tại bàn cần chọn bàn");
        }
        Draft draft = new Draft(toLines(request.items()), request.orderType(), OrderChannel.COUNTER, table, null,
                request.customerPhone(), request.customerName(), null, request.delivery(), null,
                request.paymentMethod(), Boolean.TRUE.equals(request.paidNow()), request.voucherCode(), request.note());
        Order order = place(draft);
        order.setStatus(OrderStatus.CONFIRMED);
        return publish(order);
    }

    private record Draft(List<PricingService.InputLine> lines, OrderType type, OrderChannel channel, DiningTable table,
                         Cart cart, String customerPhone, String customerName, Long addressId,
                         DeliveryAddressRequest delivery, LocalDateTime pickupTime, PaymentMethod paymentMethod,
                         boolean paidNow, String voucherCode, String note) {
    }

    private Order place(Draft d) {
        LocalDateTime now = LocalDateTime.now();
        if (d.pickupTime() != null && d.pickupTime().isBefore(now.minusMinutes(1))) {
            throw new BadRequestException("Thời gian nhận hàng phải ở tương lai");
        }
        var quote = pricingService.quote(d.lines(), d.type(), phoneOfCurrentCustomer(d.customerPhone()), d.voucherCode(), now);
        if (!quote.unavailable().isEmpty()) {
            throw new BadRequestException("Món đã hết: " + String.join(", ", quote.unavailable()));
        }
        stockService.requireStock(d.lines());
        if (quote.voucherMessage() != null) throw new BadRequestException(quote.voucherMessage());

        // a logged-in customer orders as themselves; guests are identified by the phone they type
        Customer customer = CurrentUser.customerId() != null
                ? customerService.byId(CurrentUser.customerId())
                : customerService.findOrCreate(d.customerPhone(), d.customerName());
        CustomerAddress address = d.type() == OrderType.DELIVERY
                ? customerService.resolveAddress(customer, d.addressId(), d.delivery()) : null;

        Order order = Order.builder()
                .orderCode(newOrderCode())
                .customer(customer)
                .table(d.table())
                .cart(d.cart())
                .promotion(quote.appliedPromotion())
                .orderType(d.type())
                .channel(d.channel())
                .subtotal(quote.subtotal())
                .discountAmount(quote.discountAmount())
                .totalAmount(quote.total())
                .note(d.note())
                .pickupTime(d.pickupTime())
                .orderedAt(now)
                .build();
        for (var line : quote.lines()) {
            order.getItems().add(OrderItem.builder().order(order).coffee(line.coffee()).quantity(line.quantity())
                    .unitPrice(line.unitPrice()).lineTotal(line.lineTotal()).note(line.note()).build());
            if (line.promoItem() != null) {
                line.promoItem().setSoldQuantity(line.promoItem().getSoldQuantity() + line.quantity());
            }
        }
        order = orderRepository.save(order);

        if (quote.voucher() != null) {
            quote.voucher().setStatus(VoucherStatus.USED);
            quote.voucher().setOrder(order);
        }

        // Cash is collected on hand-over; other methods are settled online (gateway simulated).
        boolean paid = d.paidNow() || d.paymentMethod() != PaymentMethod.CASH;
        Payment payment = paymentRepository.save(Payment.builder()
                .order(order).method(d.paymentMethod()).amount(quote.total())
                .status(paid ? PaymentStatus.PAID : PaymentStatus.PENDING)
                .paidAt(paid ? now : null)
                .transactionCode(paid && d.paymentMethod() != PaymentMethod.CASH ? "SIM-" + order.getOrderCode() : null)
                .build());
        order.getPayments().add(payment);

        if (address != null) {
            Shipment shipment = shipmentRepository.save(Shipment.builder()
                    .order(order).address(address).shippingFee(quote.shippingFee()).build());
            order.setShipment(shipment);
        }
        if (d.table() != null) d.table().setStatus(TableStatus.OCCUPIED);
        return order;
    }

    // ---- workflow --------------------------------------------------------

    @Transactional
    public OrderResponse confirm(Long id) {
        Order order = get(id);
        expect(order, OrderStatus.PENDING);
        order.setStatus(OrderStatus.CONFIRMED);
        return publish(order);
    }

    @Transactional
    public OrderResponse reject(Long id, String reason) {
        Order order = get(id);
        expect(order, OrderStatus.PENDING);
        close(order, OrderStatus.REJECTED, reason);
        return publish(order);
    }

    /** Cancel by staff (before the drinks are made). */
    @Transactional
    public OrderResponse cancel(Long id, String reason) {
        Order order = get(id);
        expect(order, OrderStatus.PENDING, OrderStatus.CONFIRMED);
        close(order, OrderStatus.CANCELLED, reason);
        return publish(order);
    }

    /** Cancel by the customer from the tracking page – only while the shop has not confirmed it. */
    @Transactional
    public OrderResponse cancelByCustomer(String code, String reason) {
        Order order = getByCode(code);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Đơn đã được cửa hàng xác nhận, vui lòng liên hệ nhân viên để hủy");
        }
        close(order, OrderStatus.CANCELLED, reason);
        return publish(order);
    }

    @Transactional
    public OrderResponse startPreparing(Long id) {
        Order order = get(id);
        expect(order, OrderStatus.CONFIRMED);
        inventoryService.consumeForOrder(order, staffService.current());
        order.setStatus(OrderStatus.PREPARING);
        return publish(order);
    }

    @Transactional
    public OrderResponse markReady(Long id) {
        Order order = get(id);
        expect(order, OrderStatus.PREPARING);
        order.setStatus(OrderStatus.READY);
        return publish(order);
    }

    /** Hand a delivery order to the driver. */
    @Transactional
    public OrderResponse dispatch(Long id) {
        Order order = get(id);
        expect(order, OrderStatus.READY);
        if (order.getOrderType() != OrderType.DELIVERY) throw new BadRequestException("Chỉ đơn giao hàng mới bàn giao tài xế");
        Shipment shipment = order.getShipment();
        if (shipment.getStatus() == ShipmentStatus.PENDING) {
            throw new BadRequestException("Chưa đặt xe cho đơn này");
        }
        shipment.setStatus(ShipmentStatus.DELIVERING);
        order.setStatus(OrderStatus.DELIVERING);
        return publish(order);
    }

    @Transactional
    public OrderResponse complete(Long id) {
        Order order = get(id);
        if (order.getOrderType() == OrderType.DELIVERY) expect(order, OrderStatus.DELIVERING);
        else expect(order, OrderStatus.READY);
        completeInternal(order);
        return publish(order);
    }

    /** Called when the carrier reports the delivery done. */
    void completeDelivered(Order order) {
        if (order.getStatus() == OrderStatus.DELIVERING) {
            completeInternal(order);
            publish(order);
        }
    }

    private void completeInternal(Order order) {
        LocalDateTime now = LocalDateTime.now();
        // "thu tiền nếu chưa thanh toán": hand-over means cash was collected
        order.getPayments().stream().filter(p -> p.getStatus() == PaymentStatus.PENDING).forEach(p -> {
            p.setStatus(PaymentStatus.PAID);
            p.setPaidAt(now);
        });
        if (order.getShipment() != null && order.getShipment().getDeliveredAt() == null) {
            order.getShipment().setStatus(ShipmentStatus.DELIVERED);
            order.getShipment().setDeliveredAt(now);
        }
        order.setStatus(OrderStatus.COMPLETED);
        Customer customer = order.getCustomer();
        if (customer != null) {
            int points = order.getTotalAmount().divide(vndPerPoint, 0, RoundingMode.FLOOR).intValue();
            if (points > 0) {
                customer.setLoyaltyPoints(customer.getLoyaltyPoints() + points);
                loyaltyRepository.save(LoyaltyPointHistory.builder().customer(customer).order(order)
                        .pointsChange(points).reason("Hoàn tất đơn " + order.getOrderCode()).build());
            }
        }
        releaseTable(order);
    }

    // ---- payments --------------------------------------------------------

    /** Staff collects the pending (cash) payment. */
    @Transactional
    public OrderResponse collectPayment(Long id) {
        Order order = get(id);
        if (!ACTIVE.contains(order.getStatus())) throw new BadRequestException("Đơn đã đóng");
        var pending = order.getPayments().stream().filter(p -> p.getStatus() == PaymentStatus.PENDING).toList();
        if (pending.isEmpty()) throw new BadRequestException("Đơn không có khoản cần thu");
        pending.forEach(p -> {
            p.setStatus(PaymentStatus.PAID);
            p.setPaidAt(LocalDateTime.now());
        });
        return publish(order);
    }

    @Transactional
    public PaymentResponse refund(Long paymentId, BigDecimal amount) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thanh toán: " + paymentId));
        if (payment.getStatus() != PaymentStatus.PAID && payment.getStatus() != PaymentStatus.REFUNDED) {
            throw new BadRequestException("Chỉ hoàn tiền cho khoản đã thanh toán");
        }
        BigDecimal refunded = payment.getRefundAmount().add(amount);
        if (refunded.compareTo(payment.getAmount()) > 0) throw new BadRequestException("Số tiền hoàn vượt quá số đã thu");
        payment.setRefundAmount(refunded);
        if (refunded.compareTo(payment.getAmount()) == 0) payment.setStatus(PaymentStatus.REFUNDED);
        return PaymentResponse.from(payment);
    }

    // ---- helpers ---------------------------------------------------------

    /** Reject / cancel: refund what was paid, give back the voucher and the promotion quota, free the table. */
    private void close(Order order, OrderStatus status, String reason) {
        order.setStatus(status);
        order.setCancelReason(reason);
        for (Payment p : order.getPayments()) {
            if (p.getStatus() == PaymentStatus.PAID) {
                p.setRefundAmount(p.getAmount());
                p.setStatus(PaymentStatus.REFUNDED);
            } else if (p.getStatus() == PaymentStatus.PENDING) {
                p.setStatus(PaymentStatus.FAILED);
            }
        }
        voucherRepository.findByOrderId(order.getId()).forEach(v -> {
                    v.setOrder(null);
                    v.setStatus(VoucherStatus.ISSUED);
                });
        for (OrderItem item : order.getItems()) {
            pricingService.findPromotionItemUsed(item.getCoffee(), item.getUnitPrice(), order.getOrderedAt())
                    .ifPresent(pi -> pi.setSoldQuantity(Math.max(0, pi.getSoldQuantity() - item.getQuantity())));
        }
        if (order.getShipment() != null) order.getShipment().setStatus(ShipmentStatus.FAILED);
        releaseTable(order);
    }

    private void releaseTable(Order order) {
        DiningTable table = order.getTable();
        if (table == null || table.getStatus() != TableStatus.OCCUPIED) return;
        if (!orderRepository.existsByTableIdAndStatusInAndIdNot(table.getId(), ACTIVE, order.getId())) table.setStatus(TableStatus.AVAILABLE);
    }

    /** Logged-in customers use the phone of their own profile (vouchers are tied to it). */
    private String phoneOfCurrentCustomer(String typed) {
        Long id = CurrentUser.customerId();
        return id == null ? typed : customerService.byId(id).getPhone();
    }

    private List<PricingService.InputLine> toLines(List<CartItemRequest> items) {
        return items.stream().map(i -> new PricingService.InputLine(coffeeService.get(i.coffeeId()), i.quantity(),
                i.note() == null || i.note().isBlank() ? null : i.note().trim())).toList();
    }

    private OrderResponse publish(Order order) {
        Staff actor = staffService.current();
        if (actor != null) order.setStaff(actor); // nhân viên thao tác gần nhất (không có phân công)
        OrderResponse response = OrderResponse.from(order);
        notificationService.notifyOrderChanged(response);
        return response;
    }

    private static void expect(Order order, OrderStatus... allowed) {
        for (OrderStatus s : allowed) if (order.getStatus() == s) return;
        throw new BadRequestException("Không thể thực hiện khi đơn đang ở trạng thái " + order.getStatus());
    }

    private String newOrderCode() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        String code;
        do {
            StringBuilder sb = new StringBuilder("CH").append(date);
            for (int i = 0; i < 4; i++) sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
            code = sb.toString();
        } while (orderRepository.findByOrderCode(code).isPresent());
        return code;
    }

    Order get(Long id) {
        return orderRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn: " + id));
    }

    private Order getByCode(String code) {
        return orderRepository.findByOrderCode(code.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn: " + code));
    }
}
