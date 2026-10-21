package com.coffeeshop.controller;

import com.coffeeshop.dto.response.PaymentTransactionResponse;
import com.coffeeshop.entity.PaymentTransaction;
import com.coffeeshop.entity.enums.PaymentTransactionStatus;
import com.coffeeshop.security.Perm;
import com.coffeeshop.service.OnlinePaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/** Thanh toán online qua VNPAY (public: tạo link, IPN, return, trang giả lập) và đối soát giao dịch (quản lý). */
@RestController
@RequiredArgsConstructor
public class OnlinePaymentController {

    private final OnlinePaymentService service;

    // ---- khách ---------------------------------------------------------------

    /** Link thanh toán for an order paid online (also "Thanh toán lại" from the tracking page). */
    @PostMapping("/api/public/orders/{orderCode}/pay")
    public OnlinePaymentService.PayLink pay(@PathVariable String orderCode, HttpServletRequest request) {
        return service.createForOrder(orderCode, ip(request));
    }

    /** VNPAY → us, server to server. */
    @GetMapping("/api/public/payments/vnpay/ipn")
    public Map<String, String> ipn(@RequestParam Map<String, String> params) {
        return service.ipn(params);
    }

    /** The FE result page passes on what VNPAY put on the return URL. */
    @GetMapping("/api/public/payments/vnpay/return")
    public OnlinePaymentService.Result returned(@RequestParam Map<String, String> params, HttpServletRequest request) {
        return service.returned(params, ip(request));
    }

    /** Simulated VNPAY page (mode=simulated): pay or cancel, like the sandbox would. */
    @GetMapping(value = "/api/public/payments/sandbox", produces = MediaType.TEXT_HTML_VALUE)
    public String sandbox(@RequestParam Map<String, String> params) {
        PaymentTransaction t = service.sandboxTransaction(params);
        String hidden = params.entrySet().stream()
                .map(e -> "<input type=\"hidden\" name=\"" + esc(e.getKey()) + "\" value=\"" + esc(e.getValue()) + "\">")
                .collect(Collectors.joining());
        String amount = NumberFormat.getInstance(new Locale("vi", "VN")).format(t.getAmount()) + " đ";
        return """
                <!doctype html><html lang="vi"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
                <title>VNPAY (giả lập)</title><style>
                body{font-family:Arial,sans-serif;background:#f4f6fb;margin:0;padding:24px}
                .box{max-width:420px;margin:auto;background:#fff;border-radius:12px;padding:24px;box-shadow:0 4px 18px #0001}
                h1{color:#005baa;font-size:20px;margin:0 0 4px} .muted{color:#667;font-size:13px}
                .amt{font-size:28px;font-weight:bold;margin:16px 0} button{width:100%%;padding:12px;border:0;border-radius:8px;font-size:15px;margin-top:10px;cursor:pointer}
                .ok{background:#005baa;color:#fff} .no{background:#eee} .warn{background:#fff3cd;padding:8px;border-radius:6px;font-size:12px}
                </style></head><body><div class="box">
                <h1>Cổng thanh toán VNPAY</h1><div class="muted">Môi trường giả lập · %s</div>
                <p class="muted">Đơn vị: Coffeeholic · Mã giao dịch %s<br>%s</p>
                <div class="amt">%s</div>
                <p class="warn">Trang giả lập dùng khi chưa cấu hình mã merchant VNPAY (app.payment.vnpay.mode=simulated). Kết quả được gửi về như IPN thật.</p>
                <form method="post" action="/api/public/payments/sandbox/complete?result=00">%s<button class="ok">Thanh toán thành công</button></form>
                <form method="post" action="/api/public/payments/sandbox/complete?result=24">%s<button class="no">Hủy giao dịch</button></form>
                <form method="post" action="/api/public/payments/sandbox/complete?result=51">%s<button class="no">Tài khoản không đủ số dư</button></form>
                </div></body></html>
                """.formatted(esc(params.getOrDefault("vnp_BankCode", "Chọn phương thức")), esc(t.getTxnRef()),
                esc(params.getOrDefault("vnp_OrderInfo", "")), amount, hidden, hidden, hidden);
    }

    @PostMapping(value = "/api/public/payments/sandbox/complete", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> sandboxComplete(@RequestParam("result") String result, @RequestParam Map<String, String> params) {
        Map<String, String> vnp = params.entrySet().stream().filter(e -> e.getKey().startsWith("vnp_"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        String url = service.sandboxComplete(vnp, result);
        return ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, URI.create(url).toString()).build();
    }

    // ---- đối soát ----------------------------------------------------------------

    @GetMapping("/api/payment-transactions")
    @PreAuthorize(Perm.CAN_PAYMENT_RECONCILIATION)
    public List<PaymentTransactionResponse> search(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                   @RequestParam(required = false) PaymentTransactionStatus status,
                                                   @RequestParam(defaultValue = "false") boolean mismatch) {
        return service.search(from, to, status, mismatch);
    }

    @PostMapping("/api/payment-transactions/{id}/requery")
    @PreAuthorize(Perm.CAN_PAYMENT_RECONCILIATION)
    public PaymentTransactionResponse requery(@PathVariable Long id) {
        return service.requery(id);
    }

    @PostMapping("/api/payment-transactions/{id}/reconcile")
    @PreAuthorize(Perm.CAN_PAYMENT_RECONCILIATION)
    public PaymentTransactionResponse reconcile(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        return service.markReconciled(id, body == null ? null : body.get("note"));
    }

    private static String ip(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        return ip == null || ip.isBlank() ? request.getRemoteAddr() : ip.split(",")[0].trim();
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

}
