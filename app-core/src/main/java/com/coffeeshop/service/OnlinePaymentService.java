package com.coffeeshop.service;

import com.coffeeshop.dto.response.OrderResponse;
import com.coffeeshop.dto.response.PaymentTransactionResponse;
import com.coffeeshop.entity.Order;
import com.coffeeshop.entity.Payment;
import com.coffeeshop.entity.PaymentTransaction;
import com.coffeeshop.entity.enums.OrderStatus;
import com.coffeeshop.entity.enums.PaymentMethod;
import com.coffeeshop.entity.enums.PaymentStatus;
import com.coffeeshop.entity.enums.PaymentTransactionStatus;
import com.coffeeshop.exception.BadRequestException;
import com.coffeeshop.exception.ResourceNotFoundException;
import com.coffeeshop.integration.vnpay.VnpayGateway;
import com.coffeeshop.repository.OrderRepository;
import com.coffeeshop.repository.PaymentTransactionRepository;
import com.coffeeshop.security.CurrentUser;
import com.coffeeshop.websocket.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Thanh toán online qua VNPAY for QR / online orders paid by QR, thẻ ATM or thẻ quốc tế:
 * tạo giao dịch (link thanh toán, hết hạn sau 15 phút) → khách trả trên VNPAY → IPN (server to server) ghi nhận kết quả;
 * the return page only shows it (and asks VNPAY with querydr when the IPN could not reach us) → giao dịch quá hạn bị
 * đóng và đơn chưa trả bị hủy → hoàn tiền qua cổng khi hủy / hoàn đơn → đối soát.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnlinePaymentService {

    public static final String PROVIDER = "VNPAY";

    private final PaymentTransactionRepository txRepository;
    private final com.coffeeshop.repository.PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final VnpayGateway gateway;
    private final NotificationService notificationService;

    public record PayLink(String paymentUrl, String txnRef, LocalDateTime expireAt) {
    }

    /** what the return page shows */
    public record Result(boolean validSignature, String orderCode, PaymentTransactionStatus status, String responseCode,
                         String message, BigDecimal amount) {
    }

    /** online = paid through the gateway: QR / online orders, not cash */
    public static boolean isOnline(Order order, Payment p) {
        return order.getChannel() != com.coffeeshop.entity.enums.OrderChannel.COUNTER && p.getMethod() != PaymentMethod.CASH;
    }

    /** default VNPAY page for the method the customer picked */
    private static String bankCodeFor(PaymentMethod m) {
        return switch (m) {
            case E_WALLET -> "VNPAYQR";
            case BANK_TRANSFER -> "VNBANK";
            case CARD -> "INTCARD";
            default -> null;
        };
    }

    @Transactional
    public PayLink createForOrder(String orderCode, String ip) {
        Order order = orderRepository.findByOrderCode(orderCode.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn: " + orderCode));
        if (order.getStatus() != OrderStatus.PENDING) throw new BadRequestException("Đơn không còn chờ thanh toán");
        Payment payment = order.getPayments().stream()
                .filter(p -> p.getStatus() == PaymentStatus.PENDING && isOnline(order, p)).findFirst()
                .orElseThrow(() -> new BadRequestException("Đơn không có khoản cần thanh toán online"));
        // a new attempt replaces the previous unpaid link
        txRepository.findByPaymentIdOrderByIdDesc(payment.getId()).stream()
                .filter(t -> t.getStatus() == PaymentTransactionStatus.PENDING && "PAY".equals(t.getType()))
                .forEach(t -> {
                    t.setStatus(PaymentTransactionStatus.EXPIRED);
                    t.setCompletedAt(LocalDateTime.now());
                });
        int attempt = txRepository.findByPaymentIdOrderByIdDesc(payment.getId()).size() + 1;
        String txnRef = order.getOrderCode() + "-" + attempt;
        LocalDateTime now = LocalDateTime.now();
        String createDate = VnpayGateway.now();
        LocalDateTime expireAt = now.plusMinutes(gateway.expireMinutes());
        String expireDate = LocalDateTime.parse(createDate, VnpayGateway.VNP_DATE).plusMinutes(gateway.expireMinutes())
                .format(VnpayGateway.VNP_DATE);
        String bankCode = bankCodeFor(payment.getMethod());
        String url = gateway.payUrl(txnRef, payment.getAmount(), "Thanh toan don hang " + order.getOrderCode(), bankCode, ip,
                createDate, expireDate);
        txRepository.save(PaymentTransaction.builder().payment(payment).provider(PROVIDER).type("PAY").txnRef(txnRef)
                .amount(payment.getAmount()).bankCode(bankCode).payUrl(url).gatewayDate(createDate).expireAt(expireAt)
                .createdAt(now).build());
        return new PayLink(url, txnRef, expireAt);
    }

    /**
     * IPN: VNPAY tells the result server to server; answers {RspCode, Message} as its docs require
     * (00 ok, 01 not found, 02 already confirmed, 04 wrong amount, 97 bad signature, 99 other). Retried by VNPAY otherwise.
     */
    @Transactional
    public Map<String, String> ipn(Map<String, String> params) {
        if (!gateway.verify(params)) return rsp("97", "Invalid signature");
        PaymentTransaction t = txRepository.findByTxnRef(params.getOrDefault("vnp_TxnRef", "")).orElse(null);
        if (t == null) return rsp("01", "Order not found");
        t.setIpnCount(t.getIpnCount() + 1);
        t.setRawResponse(params.toString());
        BigDecimal paid = new BigDecimal(params.getOrDefault("vnp_Amount", "0")).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        if (paid.compareTo(t.getAmount()) != 0) return rsp("04", "invalid amount");
        if (t.getStatus() != PaymentTransactionStatus.PENDING) return rsp("02", "Order already confirmed");
        apply(t, params.get("vnp_ResponseCode"), params.get("vnp_TransactionStatus"), params.get("vnp_TransactionNo"),
                params.get("vnp_BankCode"));
        return rsp("00", "Confirm Success");
    }

    /** Return URL: shows the result; asks VNPAY (querydr) when the IPN has not arrived (e.g. running on localhost). */
    @Transactional
    public Result returned(Map<String, String> params, String ip) {
        boolean valid = gateway.verify(params);
        PaymentTransaction t = txRepository.findByTxnRef(params.getOrDefault("vnp_TxnRef", ""))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch"));
        if (valid && t.getStatus() == PaymentTransactionStatus.PENDING && !gateway.simulated()) requery(t, ip);
        String code = t.getResponseCode() != null ? t.getResponseCode() : params.get("vnp_ResponseCode");
        return new Result(valid, t.getPayment().getOrder().getOrderCode(), t.getStatus(), code,
                valid ? PaymentTransactionResponse.responseText(code) : "Sai chữ ký — kết quả không đáng tin", t.getAmount());
    }

    // ---- simulated VNPAY (mode=simulated) ----------------------------------

    /** the test payment page: checks the request like VNPAY does */
    @Transactional(readOnly = true)
    public PaymentTransaction sandboxTransaction(Map<String, String> params) {
        if (!gateway.simulated()) throw new BadRequestException("Trang thử chỉ dùng khi chạy giả lập VNPAY");
        if (!gateway.verify(params)) throw new BadRequestException("Sai chữ ký yêu cầu thanh toán");
        return txRepository.findByTxnRef(params.getOrDefault("vnp_TxnRef", ""))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch"));
    }

    /** the customer pressed pay / cancel on the test page: IPN first, then the return URL to redirect to */
    @Transactional
    public String sandboxComplete(Map<String, String> params, String responseCode) {
        PaymentTransaction t = sandboxTransaction(params);
        if (t.getExpireAt() != null && LocalDateTime.now().isAfter(t.getExpireAt())) responseCode = "11";
        Map<String, String> result = gateway.signedResult(params, responseCode, "00".equals(responseCode)
                ? String.valueOf(14_000_000 + t.getId()) : "0");
        ipn(result);
        return gateway.returnUrlWith(result);
    }

    // ---- refunds -------------------------------------------------------------

    /**
     * Hoàn tiền qua cổng for a payment made online; false when it was not paid through the gateway (refund by hand).
     * Throws when VNPAY refuses, so the order is not marked refunded.
     */
    @Transactional
    public boolean refundIfOnline(Payment payment, BigDecimal amount) {
        Refund r = tryRefund(payment, amount);
        if (r.failed()) throw new BadRequestException(r.error());
        return r.online();
    }

    /**
     * Kết quả hoàn tiền qua cổng: {@code online} = khoản này trả qua VNPAY; {@code error} khác null = VNPAY từ chối /
     * không phản hồi (giao dịch hoàn vẫn được ghi lại để đối soát); {@code ref} = mã giao dịch hoàn của VNPAY.
     */
    public record Refund(boolean online, String error, String ref) {
        public boolean failed() {
            return error != null;
        }
    }

    /** Như {@link #refundIfOnline} nhưng không ném lỗi — để từ chối / hủy đơn vẫn đi tiếp khi cổng không hoàn được. */
    @Transactional
    public Refund tryRefund(Payment payment, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) return new Refund(false, null, null);
        PaymentTransaction paid = txRepository.findByPaymentIdOrderByIdDesc(payment.getId()).stream()
                .filter(t -> "PAY".equals(t.getType()) && t.getStatus() == PaymentTransactionStatus.SUCCESS).findFirst().orElse(null);
        if (paid == null) return new Refund(false, null, null);
        boolean full = amount.compareTo(paid.getAmount()) >= 0 && payment.getRefundAmount().signum() == 0;
        String ref = paid.getTxnRef() + "-R" + (txRepository.findByPaymentIdOrderByIdDesc(payment.getId()).size());
        PaymentTransaction refund = txRepository.save(PaymentTransaction.builder().payment(payment).provider(PROVIDER)
                .type("REFUND").txnRef(ref).amount(amount).bankCode(paid.getBankCode()).gatewayDate(VnpayGateway.now())
                .createdAt(LocalDateTime.now()).build());
        VnpayGateway.Answer a;
        try {
            a = gateway.simulated()
                    ? new VnpayGateway.Answer("00", "05", "R" + paid.getProviderTxnNo(), paid.getBankCode(), "simulated refund")
                    : gateway.refund(paid.getTxnRef(), amount, full, paid.getProviderTxnNo(), paid.getGatewayDate(),
                    CurrentUser.username() == null ? "system" : CurrentUser.username(), "127.0.0.1");
        } catch (RuntimeException e) {
            log.warn("VNPAY refund {} failed: {}", ref, e.getMessage());
            refund.setStatus(PaymentTransactionStatus.FAILED);
            refund.setCompletedAt(LocalDateTime.now());
            return new Refund(true, "VNPAY không phản hồi khi hoàn tiền", null);
        }
        refund.setResponseCode(a.responseCode());
        refund.setTransactionStatus(a.transactionStatus());
        refund.setProviderTxnNo(a.transactionNo());
        refund.setRawResponse(a.raw());
        refund.setCompletedAt(LocalDateTime.now());
        refund.setStatus(a.ok() ? PaymentTransactionStatus.SUCCESS : PaymentTransactionStatus.FAILED);
        if (!a.ok()) return new Refund(true, "VNPAY không hoàn tiền được: " + PaymentTransactionResponse.responseText(a.responseCode()), null);
        return new Refund(true, null, PROVIDER + "-" + a.transactionNo());
    }

    /** the order is closed: links still open are closed too */
    @Transactional
    public void closePending(Payment payment) {
        txRepository.findByPaymentIdOrderByIdDesc(payment.getId()).stream()
                .filter(t -> t.getStatus() == PaymentTransactionStatus.PENDING)
                .forEach(t -> {
                    t.setStatus(PaymentTransactionStatus.EXPIRED);
                    t.setCompletedAt(LocalDateTime.now());
                });
    }

    // ---- job: giao dịch quá hạn ------------------------------------------------

    /**
     * Links past their expiry: asked once more, then closed. Returns the orders still unpaid, to be cancelled —
     * those plus orders left unpaid past the same window with no open link (link never opened, or payment failed).
     */
    @Transactional
    public List<Long> expire() {
        Set<Long> orders = new TreeSet<>();
        LocalDateTime now = LocalDateTime.now();
        for (PaymentTransaction t : txRepository.findExpired(now)) {
            if (!gateway.simulated()) requery(t, "127.0.0.1");
            if (t.getStatus() == PaymentTransactionStatus.PENDING) {
                t.setStatus(PaymentTransactionStatus.EXPIRED);
                t.setCompletedAt(LocalDateTime.now());
            }
            Payment p = t.getPayment();
            if (p.getStatus() == PaymentStatus.PENDING && p.getOrder().getStatus() == OrderStatus.PENDING) orders.add(p.getOrder().getId());
        }
        orders.addAll(txRepository.findUnpaidOnlineOrders(now.minusMinutes(gateway.expireMinutes()), now));
        return new ArrayList<>(orders);
    }

    // ---- đối soát -------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<PaymentTransactionResponse> search(LocalDate from, LocalDate to, PaymentTransactionStatus status, boolean mismatchOnly) {
        return txRepository.search(from.atStartOfDay(), to.plusDays(1).atStartOfDay(), status).stream()
                .map(t -> PaymentTransactionResponse.from(t, mismatch(t)))
                .filter(r -> !mismatchOnly || r.mismatch() != null).toList();
    }

    /** Truy vấn lại VNPAY (querydr) and apply what it says. */
    @Transactional
    public PaymentTransactionResponse requery(Long id) {
        PaymentTransaction t = txRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch: " + id));
        if (!"PAY".equals(t.getType())) throw new BadRequestException("Chỉ truy vấn giao dịch thanh toán");
        if (gateway.simulated()) {
            t.setRawResponse("simulated querydr: " + t.getStatus());
        } else {
            requery(t, "127.0.0.1");
        }
        return PaymentTransactionResponse.from(t, mismatch(t));
    }

    @Transactional
    public PaymentTransactionResponse markReconciled(Long id, String note) {
        PaymentTransaction t = txRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch: " + id));
        if (t.getStatus() == PaymentTransactionStatus.PENDING) throw new BadRequestException("Giao dịch còn đang chờ, chưa đối soát được");
        t.setReconciled(true);
        t.setReconciledBy(CurrentUser.username());
        t.setReconciledAt(LocalDateTime.now());
        t.setReconcileNote(note == null || note.isBlank() ? null : note.trim());
        return PaymentTransactionResponse.from(t, mismatch(t));
    }

    // ---- helpers ---------------------------------------------------------------

    private void requery(PaymentTransaction t, String ip) {
        VnpayGateway.Answer a = gateway.query(t.getTxnRef(), t.getGatewayDate(), ip);
        t.setRawResponse(a.raw());
        if (!a.ok() || a.transactionStatus() == null) return;
        if (t.getStatus() == PaymentTransactionStatus.PENDING || t.getStatus() == PaymentTransactionStatus.EXPIRED) {
            if ("00".equals(a.transactionStatus())) apply(t, "00", "00", a.transactionNo(), a.bankCode());
            else if (!"01".equals(a.transactionStatus())) apply(t, a.responseCode(), a.transactionStatus(), a.transactionNo(), a.bankCode());
        }
    }

    private void apply(PaymentTransaction t, String responseCode, String transactionStatus, String transactionNo, String bankCode) {
        LocalDateTime now = LocalDateTime.now();
        t.setResponseCode(responseCode);
        t.setTransactionStatus(transactionStatus);
        t.setProviderTxnNo(transactionNo);
        if (bankCode != null) t.setBankCode(bankCode);
        t.setCompletedAt(now);
        boolean ok = "00".equals(responseCode) && "00".equals(transactionStatus);
        t.setStatus(ok ? PaymentTransactionStatus.SUCCESS : PaymentTransactionStatus.FAILED);
        if (!ok) return;
        Payment p = t.getPayment();
        if (p.getStatus() == PaymentStatus.PENDING) {
            p.setStatus(PaymentStatus.PAID);
            p.setPaidAt(now);
            p.setTransactionCode(PROVIDER + "-" + transactionNo);
            notificationService.notifyOrderChanged(OrderResponse.from(p.getOrder()));
        } else if (p.getStatus() == PaymentStatus.FAILED
                && (p.getOrder().getStatus() == OrderStatus.REJECTED || p.getOrder().getStatus() == OrderStatus.CANCELLED)) {
            // khách trả xong sau khi đơn đã bị từ chối / hủy: ghi nhận tiền vào rồi hoàn lại ngay
            log.warn("VNPAY paid {} after order {} was closed: refunding", t.getTxnRef(), p.getOrder().getOrderCode());
            p.setStatus(PaymentStatus.PAID);
            p.setPaidAt(now);
            p.setTransactionCode(PROVIDER + "-" + transactionNo);
            settleRefund(p, p.getAmount(), tryRefund(p, p.getAmount()));
            notificationService.notifyOrderChanged(OrderResponse.from(p.getOrder()));
        } else {
            log.warn("VNPAY paid {} but payment #{} is {}", t.getTxnRef(), p.getId(), p.getStatus());
        }
    }

    /**
     * Ghi kết quả hoàn {@code amount} vào khoản thanh toán: hoàn được → cộng số đã hoàn (đủ thì REFUNDED); cổng từ chối →
     * REFUND_PENDING để nhân viên chuyển khoản tay ({@link #markRefundedManually}).
     */
    public void settleRefund(Payment p, BigDecimal amount, Refund r) {
        if (r.failed()) {
            p.setStatus(PaymentStatus.REFUND_PENDING);
            p.setRefundNote(r.error() + " — cần chuyển khoản trả khách " + amount.stripTrailingZeros().toPlainString() + "đ");
            return;
        }
        p.setRefundAmount(p.getRefundAmount().add(amount).min(p.getAmount()));
        if (p.getRefundAmount().compareTo(p.getAmount()) >= 0) p.setStatus(PaymentStatus.REFUNDED);
        p.setRefundedAt(LocalDateTime.now());
        if (r.ref() != null) p.setRefundRef(r.ref());
    }

    /** nhân viên đã chuyển khoản trả khách cho khoản đang chờ hoàn tay */
    @Transactional
    public Payment markRefundedManually(Long paymentId, String reference, String note) {
        Payment p = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new com.coffeeshop.exception.ResourceNotFoundException("Không tìm thấy thanh toán: " + paymentId));
        if (p.getStatus() != PaymentStatus.REFUND_PENDING) throw new BadRequestException("Khoản này không ở trạng thái chờ hoàn tiền");
        if (reference == null || reference.isBlank()) throw new BadRequestException("Nhập mã giao dịch chuyển khoản trả khách");
        p.setRefundAmount(p.getAmount());
        p.setStatus(PaymentStatus.REFUNDED);
        p.setRefundedAt(LocalDateTime.now());
        p.setRefundRef(reference.trim());
        String by = CurrentUser.username() == null ? "" : " (" + CurrentUser.username() + ")";
        p.setRefundNote((p.getRefundNote() == null ? "" : p.getRefundNote() + "\n") + "Đã hoàn tay" + by
                + (note == null || note.isBlank() ? "" : ": " + note.trim()));
        notificationService.notifyOrderChanged(OrderResponse.from(p.getOrder()));
        return p;
    }

    /** thử hoàn lại qua VNPAY cho khoản đang chờ hoàn tay */
    @Transactional
    public Payment retryRefund(Long paymentId) {
        Payment p = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new com.coffeeshop.exception.ResourceNotFoundException("Không tìm thấy thanh toán: " + paymentId));
        if (p.getStatus() != PaymentStatus.REFUND_PENDING) throw new BadRequestException("Khoản này không ở trạng thái chờ hoàn tiền");
        BigDecimal left = p.getAmount().subtract(p.getRefundAmount());
        Refund r = tryRefund(p, left);
        if (r.failed()) {
            p.setRefundNote(r.error() + " — cần chuyển khoản trả khách " + left.stripTrailingZeros().toPlainString() + "đ");
            return p;
        }
        settleRefund(p, left, r);
        notificationService.notifyOrderChanged(OrderResponse.from(p.getOrder()));
        return p;
    }

    /** các khoản đang chờ hoàn tay, cũ nhất trước */
    @Transactional(readOnly = true)
    public List<Payment> refundPending() {
        return paymentRepository.findByStatusOrderByIdAsc(PaymentStatus.REFUND_PENDING);
    }

    /** null when the gateway and the shop agree */
    private String mismatch(PaymentTransaction t) {
        Payment p = t.getPayment();
        if (t.getAmount().compareTo("PAY".equals(t.getType()) ? p.getAmount() : t.getAmount()) != 0) return "Số tiền giao dịch khác số tiền đơn";
        if ("PAY".equals(t.getType()) && t.getStatus() == PaymentTransactionStatus.SUCCESS
                && p.getStatus() != PaymentStatus.PAID && p.getStatus() != PaymentStatus.REFUNDED) {
            return "VNPAY báo đã thu nhưng đơn chưa ghi nhận thanh toán";
        }
        if (t.getStatus() == PaymentTransactionStatus.SUCCESS && (t.getProviderTxnNo() == null || t.getProviderTxnNo().isBlank())) {
            return "Thành công nhưng thiếu mã giao dịch VNPAY";
        }
        if (t.getStatus() == PaymentTransactionStatus.PENDING && t.getExpireAt() != null && LocalDateTime.now().isAfter(t.getExpireAt().plusMinutes(5))) {
            return "Quá hạn mà chưa có kết quả";
        }
        if ("REFUND".equals(t.getType()) && t.getStatus() == PaymentTransactionStatus.FAILED) return "Hoàn tiền thất bại — cần hoàn tay";
        if ("PAY".equals(t.getType()) && t.getStatus() == PaymentTransactionStatus.SUCCESS && p.getOrder().getStatus() == OrderStatus.CANCELLED
                && p.getRefundAmount().compareTo(p.getAmount()) < 0) {
            return "Đơn đã hủy nhưng chưa hoàn đủ tiền";
        }
        return null;
    }

    private static Map<String, String> rsp(String code, String message) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("RspCode", code);
        m.put("Message", message);
        return m;
    }

}
