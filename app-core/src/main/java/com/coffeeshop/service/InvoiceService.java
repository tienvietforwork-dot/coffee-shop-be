package com.coffeeshop.service;

import com.coffeeshop.dto.response.InvoiceResponse;
import com.coffeeshop.entity.Invoice;
import com.coffeeshop.entity.InvoiceLine;
import com.coffeeshop.entity.Order;
import com.coffeeshop.entity.OrderItem;
import com.coffeeshop.entity.enums.OrderStatus;
import com.coffeeshop.entity.enums.PaymentMethod;
import com.coffeeshop.exception.BadRequestException;
import com.coffeeshop.exception.ResourceNotFoundException;
import com.coffeeshop.integration.einvoice.EinvoiceProperties;
import com.coffeeshop.integration.einvoice.EinvoiceProvider;
import com.coffeeshop.repository.InvoiceRepository;
import com.coffeeshop.repository.OrderRepository;
import com.coffeeshop.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Hóa đơn điện tử khởi tạo từ máy tính tiền: lập cho từng đơn khi đơn hoàn tất (đã thu tiền) — khách không cần yêu cầu;
 * thông tin người mua chỉ ghi khi khách đưa (tên công ty, MST…). The job sends the queue to the provider (retries, then
 * FAILED for a person to look at). After issuing: wrong buyer details → hóa đơn thay thế; money returned on a completed
 * order → hóa đơn điều chỉnh giảm. Never cancelled or deleted (NĐ 70/2025 → NĐ 254/2026).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final OrderRepository orderRepository;
    private final EinvoiceProvider provider;

    public record Buyer(String buyerName, String buyerCompany, String buyerTaxCode, String buyerAddress, String buyerEmail,
                        String buyerPhone, String buyerIdNumber) {
    }

    // ---- queries ---------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<InvoiceResponse> search(LocalDate from, LocalDate to, String status, String keyword) {
        String kw = keyword == null || keyword.isBlank() ? null : "%" + keyword.trim().toLowerCase() + "%";
        return invoiceRepository.search(from.atStartOfDay(), to.plusDays(1).atStartOfDay(),
                status == null || status.isBlank() ? null : status, kw).stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public InvoiceResponse find(Long id) {
        return response(get(id));
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> forOrderCode(String orderCode) {
        Order o = order(orderCode);
        return invoiceRepository.findByOrderIdOrderByIdAsc(o.getId()).stream().map(this::response).toList();
    }

    // ---- lập hóa đơn -------------------------------------------------------------

    /** A completed order goes on the queue (once). */
    @Transactional
    public void queueFor(Order order) {
        if (order.getStatus() != OrderStatus.COMPLETED) return;
        if (invoiceRepository.findByOrderIdOrderByIdAsc(order.getId()).stream().anyMatch(i -> "ORIGINAL".equals(i.getKind()))) return;
        Invoice inv = base(order, "ORIGINAL", null, null);
        addOrderLines(inv, order);
        totals(inv, order.getTotalAmount().subtract(refunded(order)));
        invoiceRepository.save(inv);
    }

    /** Thông tin xuất hóa đơn (công ty / cá nhân): on the pending invoice, or a hóa đơn thay thế once issued. */
    @Transactional
    public InvoiceResponse setBuyer(String orderCode, Buyer buyer) {
        validate(buyer);
        Order order = order(orderCode);
        Invoice current = invoiceRepository.findByOrderIdOrderByIdAsc(order.getId()).stream()
                .filter(i -> !"ADJUSTMENT".equals(i.getKind()) && !"REPLACED".equals(i.getStatus()))
                .reduce((a, b) -> b).orElse(null);
        if (current == null) {
            if (order.getStatus() != OrderStatus.COMPLETED) throw new BadRequestException("Đơn chưa hoàn tất — hóa đơn lập khi thanh toán xong");
            queueFor(order);
            current = invoiceRepository.findByOrderIdOrderByIdAsc(order.getId()).get(0);
        }
        if ("PENDING".equals(current.getStatus()) || "FAILED".equals(current.getStatus())) {
            applyBuyer(current, buyer);
            current.setStatus("PENDING");
            current.setAttempts(0);
            return response(current);
        }
        return response(replace(current, buyer, "Bổ sung / sửa thông tin người mua"));
    }

    /** Hóa đơn thay thế for an issued one (sai tên, địa chỉ, MST người mua…). */
    @Transactional
    public InvoiceResponse replace(Long id, Buyer buyer, String reason) {
        validate(buyer);
        if (reason == null || reason.isBlank()) throw new BadRequestException("Ghi lý do lập hóa đơn thay thế");
        return response(replace(get(id), buyer, reason.trim()));
    }

    /** Hóa đơn điều chỉnh giảm when money is given back on a completed, invoiced order. */
    @Transactional
    public void adjustForRefund(Order order, BigDecimal amount, String reason) {
        Invoice issued = invoiceRepository.findByOrderIdOrderByIdAsc(order.getId()).stream()
                .filter(i -> "ISSUED".equals(i.getStatus()) && !"ADJUSTMENT".equals(i.getKind())).reduce((a, b) -> b).orElse(null);
        if (issued == null) {
            // not issued yet: the pending invoice simply carries the lower amount
            invoiceRepository.findByOrderIdOrderByIdAsc(order.getId()).stream().filter(i -> "PENDING".equals(i.getStatus())).findFirst()
                    .ifPresent(i -> totals(i, i.getTotalAmount().subtract(amount)));
            return;
        }
        Invoice adj = base(order, "ADJUSTMENT", issued, reason);
        copyBuyer(issued, adj);
        adj.getLines().add(InvoiceLine.builder().invoice(adj).lineNo(1)
                .name("Điều chỉnh giảm tiền hàng theo hóa đơn số " + issued.getInvoiceNo() + " ký hiệu " + issued.getSeries() + " — " + reason)
                .unit("Lần").quantity(BigDecimal.ONE).unitPrice(amount.negate()).amount(amount.negate()).build());
        totals(adj, amount.negate());
        invoiceRepository.save(adj);
    }

    /** FAILED → back on the queue. */
    @Transactional
    public InvoiceResponse retry(Long id) {
        Invoice inv = get(id);
        if (!"FAILED".equals(inv.getStatus())) throw new BadRequestException("Chỉ gửi lại hóa đơn phát hành lỗi");
        inv.setStatus("PENDING");
        inv.setAttempts(0);
        inv.setLastError(null);
        return response(inv);
    }

    // ---- job: phát hành ------------------------------------------------------------

    @Scheduled(fixedDelayString = "${app.einvoice.issue-ms:20000}", initialDelay = 25_000)
    @Transactional
    public int issueQueued() {
        int issued = 0;
        for (Invoice inv : invoiceRepository.findQueued(provider.props().getMaxAttempts())) {
            try {
                Integer max = invoiceRepository.maxNumber(inv.getSeries());
                EinvoiceProvider.Issued r = provider.issue(inv, (max == null ? 0 : max) + 1);
                inv.setInvoiceNo(r.invoiceNo());
                inv.setTaxAuthorityCode(r.taxAuthorityCode());
                inv.setLookupCode(r.lookupCode());
                inv.setLookupUrl(provider.props().getLookupUrl());
                inv.setProviderInvoiceId(r.providerId());
                inv.setStatus("ISSUED");
                inv.setIssuedAt(LocalDateTime.now());
                inv.setLastError(null);
                if ("REPLACEMENT".equals(inv.getKind()) && inv.getRelated() != null) inv.getRelated().setStatus("REPLACED");
                invoiceRepository.flush();
                issued++;
            } catch (RuntimeException e) {
                inv.setAttempts(inv.getAttempts() + 1);
                inv.setLastError(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage().substring(0, Math.min(500, e.getMessage().length())));
                if (inv.getAttempts() >= provider.props().getMaxAttempts()) inv.setStatus("FAILED");
                log.warn("Phát hành hóa đơn đơn {} lỗi (lần {}): {}", inv.getOrder().getOrderCode(), inv.getAttempts(), e.getMessage());
            }
        }
        return issued;
    }

    // ---- helpers -------------------------------------------------------------------

    private Invoice replace(Invoice original, Buyer buyer, String reason) {
        if (!"ISSUED".equals(original.getStatus()) || "ADJUSTMENT".equals(original.getKind())) {
            throw new BadRequestException("Chỉ lập thay thế cho hóa đơn đã phát hành");
        }
        boolean pending = invoiceRepository.findByOrderIdOrderByIdAsc(original.getOrder().getId()).stream()
                .anyMatch(i -> original.equals(i.getRelated()) && "PENDING".equals(i.getStatus()));
        if (pending) throw new BadRequestException("Đang có hóa đơn thay thế chờ phát hành");
        Invoice inv = base(original.getOrder(), "REPLACEMENT", original, reason);
        applyBuyer(inv, buyer);
        int n = 1;
        for (InvoiceLine l : original.getLines()) {
            inv.getLines().add(InvoiceLine.builder().invoice(inv).lineNo(n++).name(l.getName()).unit(l.getUnit())
                    .quantity(l.getQuantity()).unitPrice(l.getUnitPrice()).amount(l.getAmount()).build());
        }
        totals(inv, original.getTotalAmount());
        return invoiceRepository.save(inv);
    }

    private Invoice base(Order order, String kind, Invoice related, String reason) {
        var p = provider.props();
        return Invoice.builder().order(order).kind(kind).related(related).status("PENDING").templateCode(p.templateCode())
                .series(p.getSeries()).vatRate(p.getVatRate()).paymentMethod(paymentName(order)).reason(reason)
                .amountBeforeTax(BigDecimal.ZERO).vatAmount(BigDecimal.ZERO).totalAmount(BigDecimal.ZERO)
                .createdBy(CurrentUser.username()).createdAt(LocalDateTime.now()).build();
    }

    /** every drink on its own line, then chiết khấu and phí giao hàng */
    private void addOrderLines(Invoice inv, Order order) {
        int n = 1;
        for (OrderItem item : order.getItems()) {
            inv.getLines().add(InvoiceLine.builder().invoice(inv).lineNo(n++)
                    .name(item.getCoffee().getName() + (item.getNote() == null ? "" : " (" + item.getNote() + ")"))
                    .unit("Ly").quantity(BigDecimal.valueOf(item.getQuantity())).unitPrice(item.getUnitPrice())
                    .amount(item.getLineTotal()).build());
        }
        if (order.getDiscountAmount().signum() > 0) {
            inv.getLines().add(InvoiceLine.builder().invoice(inv).lineNo(n++).name("Chiết khấu thương mại (khuyến mãi / voucher)")
                    .unit("Lần").quantity(BigDecimal.ONE).unitPrice(order.getDiscountAmount().negate())
                    .amount(order.getDiscountAmount().negate()).build());
        }
        if (order.getShipment() != null && order.getShipment().getShippingFee().signum() > 0) {
            BigDecimal fee = order.getShipment().getShippingFee();
            inv.getLines().add(InvoiceLine.builder().invoice(inv).lineNo(n).name("Phí giao hàng").unit("Lần")
                    .quantity(BigDecimal.ONE).unitPrice(fee).amount(fee).build());
        }
    }

    /** prices include VAT: tiền chưa thuế = tổng ÷ (1 + thuế suất) */
    private static void totals(Invoice inv, BigDecimal total) {
        inv.setTotalAmount(total.setScale(2, RoundingMode.HALF_UP));
        if (inv.getVatRate() == null || inv.getVatRate().signum() == 0) {
            inv.setAmountBeforeTax(inv.getTotalAmount());
            inv.setVatAmount(BigDecimal.ZERO);
            return;
        }
        BigDecimal before = total.multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(100).add(inv.getVatRate()), 0, RoundingMode.HALF_UP).setScale(2, RoundingMode.UNNECESSARY);
        inv.setAmountBeforeTax(before);
        inv.setVatAmount(inv.getTotalAmount().subtract(before));
    }

    private static BigDecimal refunded(Order order) {
        return order.getPayments().stream().map(p -> p.getRefundAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String paymentName(Order order) {
        return order.getPayments().stream().findFirst().map(p -> p.getMethod() == PaymentMethod.CASH ? "Tiền mặt" : "Chuyển khoản")
                .orElse("Tiền mặt");
    }

    private static void validate(Buyer b) {
        if (b == null) throw new BadRequestException("Nhập thông tin người mua");
        boolean company = b.buyerTaxCode() != null && !b.buyerTaxCode().isBlank();
        if (company) {
            if (!b.buyerTaxCode().trim().matches("^\\d{10}(-\\d{3})?$|^\\d{12}$")) throw new BadRequestException("Mã số thuế không hợp lệ");
            if (b.buyerCompany() == null || b.buyerCompany().isBlank()) throw new BadRequestException("Nhập tên đơn vị");
            if (b.buyerAddress() == null || b.buyerAddress().isBlank()) throw new BadRequestException("Nhập địa chỉ đơn vị");
        } else if ((b.buyerName() == null || b.buyerName().isBlank())) {
            throw new BadRequestException("Nhập họ tên người mua hoặc mã số thuế đơn vị");
        }
    }

    private static void applyBuyer(Invoice inv, Buyer b) {
        inv.setBuyerName(blank(b.buyerName()));
        inv.setBuyerCompany(blank(b.buyerCompany()));
        inv.setBuyerTaxCode(blank(b.buyerTaxCode()));
        inv.setBuyerAddress(blank(b.buyerAddress()));
        inv.setBuyerEmail(blank(b.buyerEmail()));
        inv.setBuyerPhone(blank(b.buyerPhone()));
        inv.setBuyerIdNumber(blank(b.buyerIdNumber()));
    }

    private static void copyBuyer(Invoice from, Invoice to) {
        to.setBuyerName(from.getBuyerName());
        to.setBuyerCompany(from.getBuyerCompany());
        to.setBuyerTaxCode(from.getBuyerTaxCode());
        to.setBuyerAddress(from.getBuyerAddress());
        to.setBuyerEmail(from.getBuyerEmail());
        to.setBuyerPhone(from.getBuyerPhone());
        to.setBuyerIdNumber(from.getBuyerIdNumber());
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private InvoiceResponse response(Invoice i) {
        var p = provider.props();
        return InvoiceResponse.from(i, p.getSellerName(), p.getSellerTaxCode(), p.getSellerAddress());
    }

    private Invoice get(Long id) {
        return invoiceRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hóa đơn: " + id));
    }

    private Order order(String code) {
        return orderRepository.findByOrderCode(code.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn: " + code));
    }
}
