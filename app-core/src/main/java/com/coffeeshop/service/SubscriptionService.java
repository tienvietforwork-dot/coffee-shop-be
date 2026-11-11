package com.coffeeshop.service;

import com.coffeeshop.dto.request.CartItemRequest;
import com.coffeeshop.dto.request.DeliveryAddressRequest;
import com.coffeeshop.entity.Customer;
import com.coffeeshop.entity.DeliverySubscription;
import com.coffeeshop.entity.DeliverySubscriptionItem;
import com.coffeeshop.entity.enums.CoffeeStatus;
import com.coffeeshop.entity.enums.SubscriptionFrequency;
import com.coffeeshop.entity.enums.SubscriptionStatus;
import com.coffeeshop.exception.BadRequestException;
import com.coffeeshop.exception.ResourceNotFoundException;
import com.coffeeshop.repository.DeliverySubscriptionRepository;
import com.coffeeshop.repository.OrderRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

/**
 * Gói giao cà phê định kỳ: khách (hoặc nhân viên thay khách) đăng ký các món, địa chỉ, lịch giao mọi ngày / theo thứ
 * trong tuần và một giờ giao cố định. {@link #runDue} (chạy định kỳ) tạo đơn giao cho lần giao sắp tới trước
 * {@code app.subscription.lead-minutes} phút, mỗi ngày tối đa 1 đơn; lỗi (hết món, ngoài khu vực…) ghi vào gói để
 * nhân viên xử lý.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final DeliverySubscriptionRepository repository;
    private final OrderRepository orderRepository;
    private final CustomerService customerService;
    private final CoffeeService coffeeService;
    private final DeliveryFeeService deliveryFeeService;
    private final OrderService orderService;

    /** tạo đơn trước giờ giao bao nhiêu phút (đủ thời gian xác nhận, pha, đi giao) */
    @Value("${app.subscription.lead-minutes:60}")
    private int leadMinutes;

    public record Request(
            @Size(max = 100) String customerName,
            @Pattern(regexp = "^0\\d{9,10}$", message = "invalid phone number") String customerPhone,
            Long addressId,
            @Valid DeliveryAddressRequest delivery,
            @NotEmpty @Valid List<CartItemRequest> items,
            @NotNull SubscriptionFrequency frequency,
            List<Integer> days,
            @NotNull LocalTime deliveryTime,
            @NotNull LocalDate startDate,
            LocalDate endDate,
            String note) {
    }

    public record Item(Long coffeeId, String coffeeName, BigDecimal price, Integer quantity, String note, boolean available) {
    }

    /** {@code nextDelivery}: lần giao kế tiếp (null = hết hạn / không hoạt động); {@code lastOrderCode}: đơn gần nhất */
    public record View(Long id, String code, Long customerId, String customerName, String customerPhone, Long addressId,
                       String recipientName, String recipientPhone, String address, SubscriptionFrequency frequency,
                       List<Integer> days, String schedule, LocalTime deliveryTime, LocalDate startDate, LocalDate endDate,
                       SubscriptionStatus status, String note, List<Item> items, BigDecimal itemsTotal,
                       LocalDateTime nextDelivery, LocalDate lastRunDate, String lastError, String lastOrderCode,
                       long orderCount) {
    }

    // ---- đọc --------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<View> findAll() {
        return repository.findAllByOrderByIdDesc().stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public List<View> ofCustomer(Long customerId) {
        return repository.findByCustomerIdOrderByIdDesc(customerId).stream().map(this::view).toList();
    }

    // ---- ghi --------------------------------------------------------------------------

    /** nhân viên đăng ký thay khách ({@code customerId} null) hoặc khách tự đăng ký ({@code customerId} = khách đang đăng nhập) */
    @Transactional
    public View create(Request r, Long customerId) {
        Customer customer = customerId != null ? customerService.byId(customerId)
                : customerService.findOrCreate(r.customerPhone(), r.customerName());
        if (customer == null) throw new BadRequestException("Nhập số điện thoại khách");
        DeliverySubscription s = DeliverySubscription.builder().code(newCode()).customer(customer).build();
        apply(s, r);
        return view(repository.save(s));
    }

    @Transactional
    public View update(Long id, Request r, Long customerId) {
        DeliverySubscription s = get(id, customerId);
        if (s.getStatus() == SubscriptionStatus.CANCELLED) throw new BadRequestException("Gói đã hủy");
        apply(s, r);
        return view(s);
    }

    @Transactional
    public View setStatus(Long id, SubscriptionStatus status, Long customerId) {
        DeliverySubscription s = get(id, customerId);
        if (s.getStatus() == SubscriptionStatus.CANCELLED) throw new BadRequestException("Gói đã hủy");
        s.setStatus(status);
        if (status == SubscriptionStatus.ACTIVE) s.setLastError(null);
        return view(s);
    }

    private void apply(DeliverySubscription s, Request r) {
        s.setAddress(customerService.resolveAddress(s.getCustomer(), r.addressId(), r.delivery()));
        // địa chỉ phải trong khu vực giao (ném lỗi nếu không)
        deliveryFeeService.rule(s.getAddress().getAddress(), s.getCustomer().getId());
        s.setFrequency(r.frequency());
        List<Integer> days = r.days() == null ? List.of() : r.days().stream().filter(Objects::nonNull)
                .filter(d -> d >= 1 && d <= 7).distinct().sorted().toList();
        if (r.frequency() == SubscriptionFrequency.WEEKLY && days.isEmpty()) throw new BadRequestException("Chọn các thứ trong tuần sẽ giao");
        s.setDays(r.frequency() == SubscriptionFrequency.WEEKLY ? String.join(",", days.stream().map(String::valueOf).toList()) : null);
        if (!deliveryFeeService.isOpenAt(r.deliveryTime())) throw new BadRequestException("Giờ giao phải trong giờ mở cửa của quán");
        s.setDeliveryTime(r.deliveryTime());
        if (r.endDate() != null && r.endDate().isBefore(r.startDate())) throw new BadRequestException("Ngày kết thúc trước ngày bắt đầu");
        s.setStartDate(r.startDate());
        s.setEndDate(r.endDate());
        s.setNote(r.note() == null || r.note().isBlank() ? null : r.note().trim());
        s.getItems().clear();
        for (CartItemRequest i : r.items()) {
            var coffee = coffeeService.get(i.coffeeId());
            if (coffee.getStatus() == CoffeeStatus.DISCONTINUED || coffee.getStatus() == CoffeeStatus.HIDDEN) {
                throw new BadRequestException("Món không còn bán: " + coffee.getName());
            }
            s.getItems().add(DeliverySubscriptionItem.builder().subscription(s).coffee(coffee).quantity(i.quantity())
                    .note(i.note() == null || i.note().isBlank() ? null : i.note().trim()).build());
        }
        if (s.nextDate(LocalDate.now(DeliveryFeeService.SHOP_ZONE)) == null) throw new BadRequestException("Lịch giao không còn ngày nào");
    }

    // ---- tạo đơn theo lịch ------------------------------------------------------------------

    /** gói đến hạn: đang hoạt động, hôm nay có giao, chưa tạo đơn hôm nay, còn ≤ leadMinutes phút tới giờ giao */
    @Transactional(readOnly = true)
    public List<Long> due(LocalDateTime now) {
        LocalDate today = now.toLocalDate();
        return repository.findByStatus(SubscriptionStatus.ACTIVE).stream()
                .filter(s -> s.runsOn(today) && !today.equals(s.getLastRunDate()))
                .filter(s -> !now.isBefore(today.atTime(s.getDeliveryTime()).minusMinutes(leadMinutes)))
                .map(DeliverySubscription::getId).toList();
    }

    /** tạo đơn cho một gói đến hạn — giao dịch riêng để một gói lỗi không ảnh hưởng gói khác */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void runDue(Long id, LocalDateTime now) {
        DeliverySubscription s = repository.findById(id).orElse(null);
        if (s == null || s.getStatus() != SubscriptionStatus.ACTIVE) return;
        LocalDate today = now.toLocalDate();
        if (today.equals(s.getLastRunDate())) return;
        s.setLastRunDate(today);
        LocalDateTime at = today.atTime(s.getDeliveryTime());
        if (at.isBefore(now)) { // quá giờ giao rồi (server tắt…): bỏ lần này
            s.setLastError("Bỏ lỡ lần giao " + at.toLocalTime() + " " + today);
            return;
        }
        try {
            var order = orderService.placeForSubscription(s.getId(), at);
            s.setLastError(null);
            log.info("Gói {}: tạo đơn {} giao lúc {}", s.getCode(), order.orderCode(), at);
        } catch (RuntimeException e) {
            s.setLastError(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage().substring(0, Math.min(250, e.getMessage().length())));
            log.warn("Gói {}: không tạo được đơn: {}", s.getCode(), e.getMessage());
        }
        if (s.getEndDate() != null && s.nextDate(today.plusDays(1)) == null) s.setStatus(SubscriptionStatus.CANCELLED);
    }

    // ---- helpers ---------------------------------------------------------------------

    private View view(DeliverySubscription s) {
        LocalDateTime now = LocalDateTime.now(DeliveryFeeService.SHOP_ZONE);
        LocalDateTime next = null;
        if (s.getStatus() == SubscriptionStatus.ACTIVE) {
            LocalDate from = now.toLocalDate();
            // hôm nay đã tạo đơn hoặc đã quá giờ giao: từ ngày mai
            if (from.equals(s.getLastRunDate()) || now.toLocalTime().isAfter(s.getDeliveryTime())) from = from.plusDays(1);
            LocalDate d = s.nextDate(from);
            next = d == null ? null : d.atTime(s.getDeliveryTime());
        }
        var items = s.getItems().stream().map(i -> new Item(i.getCoffee().getId(), i.getCoffee().getName(), i.getCoffee().getPrice(),
                i.getQuantity(), i.getNote(), i.getCoffee().getStatus() == CoffeeStatus.AVAILABLE)).toList();
        BigDecimal total = items.stream().map(i -> i.price().multiply(BigDecimal.valueOf(i.quantity()))).reduce(BigDecimal.ZERO, BigDecimal::add);
        var last = orderRepository.findFirstBySubscriptionIdOrderByIdDesc(s.getId()).orElse(null);
        String schedule = (s.getFrequency() == SubscriptionFrequency.DAILY ? "Mỗi ngày" : DeliverySubscription.dayText(s.dayList()))
                + " lúc " + s.getDeliveryTime();
        return new View(s.getId(), s.getCode(), s.getCustomer().getId(), s.getCustomer().getFullName(), s.getCustomer().getPhone(),
                s.getAddress().getId(), s.getAddress().getRecipientName(), s.getAddress().getRecipientPhone(), s.getAddress().getAddress(),
                s.getFrequency(), s.dayList(), schedule, s.getDeliveryTime(), s.getStartDate(), s.getEndDate(), s.getStatus(), s.getNote(),
                items, total, next, s.getLastRunDate(), s.getLastError(), last == null ? null : last.getOrderCode(),
                orderRepository.countBySubscriptionId(s.getId()));
    }

    /** {@code customerId} khác null: chỉ gói của khách đó */
    private DeliverySubscription get(Long id, Long customerId) {
        DeliverySubscription s = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gói: " + id));
        if (customerId != null && !s.getCustomer().getId().equals(customerId)) throw new ResourceNotFoundException("Không tìm thấy gói: " + id);
        return s;
    }

    private String newCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder("GD");
            for (int i = 0; i < 6; i++) sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
            code = sb.toString();
        } while (repository.existsByCode(code));
        return code;
    }
}
