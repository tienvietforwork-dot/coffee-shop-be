package com.coffeeshop.dto.response;

import com.coffeeshop.entity.PaymentTransaction;
import com.coffeeshop.entity.enums.PaymentMethod;
import com.coffeeshop.entity.enums.PaymentStatus;
import com.coffeeshop.entity.enums.PaymentTransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A gateway transaction for đối soát, with the order payment it belongs to. {@code mismatch}: why it needs a look
 * (null when gateway and shop agree).
 */
public record PaymentTransactionResponse(Long id, String provider, String type, String txnRef, BigDecimal amount,
                                         PaymentTransactionStatus status, String bankCode, String providerTxnNo,
                                         String responseCode, String responseText, LocalDateTime createdAt,
                                         LocalDateTime completedAt, LocalDateTime expireAt, int ipnCount,
                                         Long orderId, String orderCode, Long paymentId, PaymentMethod paymentMethod,
                                         BigDecimal paymentAmount, PaymentStatus paymentStatus, String mismatch,
                                         boolean reconciled, String reconciledBy, LocalDateTime reconciledAt,
                                         String reconcileNote) {

    public static PaymentTransactionResponse from(PaymentTransaction t, String mismatch) {
        var p = t.getPayment();
        return new PaymentTransactionResponse(t.getId(), t.getProvider(), t.getType(), t.getTxnRef(), t.getAmount(),
                t.getStatus(), t.getBankCode(), t.getProviderTxnNo(), t.getResponseCode(), responseText(t.getResponseCode()),
                t.getCreatedAt(), t.getCompletedAt(), t.getExpireAt(), t.getIpnCount(), p.getOrder().getId(),
                p.getOrder().getOrderCode(), p.getId(), p.getMethod(), p.getAmount(), p.getStatus(), mismatch,
                t.isReconciled(), t.getReconciledBy(), t.getReconciledAt(), t.getReconcileNote());
    }

    /** vnp_ResponseCode → what it means (sandbox.vnpayment.vn bảng mã lỗi) */
    public static String responseText(String code) {
        if (code == null) return null;
        return switch (code) {
            case "00" -> "Giao dịch thành công";
            case "07" -> "Trừ tiền thành công, giao dịch bị nghi ngờ";
            case "09" -> "Thẻ / tài khoản chưa đăng ký Internet Banking";
            case "10" -> "Xác thực thông tin thẻ sai quá 3 lần";
            case "11" -> "Hết hạn chờ thanh toán";
            case "12" -> "Thẻ / tài khoản bị khóa";
            case "13" -> "Sai mật khẩu xác thực (OTP)";
            case "24" -> "Khách hủy giao dịch";
            case "51" -> "Tài khoản không đủ số dư";
            case "65" -> "Vượt hạn mức giao dịch trong ngày";
            case "75" -> "Ngân hàng đang bảo trì";
            case "79" -> "Nhập sai mật khẩu thanh toán quá số lần quy định";
            case "91" -> "Không tìm thấy giao dịch";
            case "94" -> "Yêu cầu bị trùng / đang xử lý";
            default -> "Lỗi khác (" + code + ")";
        };
    }
}
