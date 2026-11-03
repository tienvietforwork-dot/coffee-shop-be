package com.coffeeshop.entity;

import com.coffeeshop.entity.enums.PaymentTransactionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One attempt with the payment gateway (VNPAY): a PAY for an order payment, or a REFUND of one.
 * {@code txnRef} is our reference (vnp_TxnRef, unique), {@code providerTxnNo} the gateway's (vnp_TransactionNo).
 * The gateway's answers are kept as received for đối soát; {@code reconciled} is set by the accountant.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "payment_transactions")
public class PaymentTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;
    @Column(nullable = false, length = 20)
    private String provider;
    /** PAY or REFUND */
    @Column(nullable = false, length = 10)
    private String type;
    @Column(name = "txn_ref", nullable = false, length = 40, unique = true)
    private String txnRef;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentTransactionStatus status = PaymentTransactionStatus.PENDING;
    /** VNPAYQR, VNBANK, INTCARD… asked for / used */
    @Column(name = "bank_code", length = 20)
    private String bankCode;
    @Column(name = "provider_txn_no", length = 40)
    private String providerTxnNo;
    /** vnp_ResponseCode */
    @Column(name = "response_code", length = 10)
    private String responseCode;
    /** vnp_TransactionStatus */
    @Column(name = "transaction_status", length = 10)
    private String transactionStatus;
    @Column(name = "pay_url", columnDefinition = "TEXT")
    private String payUrl;
    /** vnp_CreateDate, needed to query / refund */
    @Column(name = "gateway_date", length = 14)
    private String gatewayDate;
    @Column(name = "expire_at")
    private LocalDateTime expireAt;
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    /** last callback / query answer as received */
    @Column(name = "raw_response", columnDefinition = "TEXT")
    private String rawResponse;
    @Builder.Default
    @Column(name = "ipn_count", nullable = false)
    private int ipnCount = 0;
    @Builder.Default
    @Column(nullable = false)
    private boolean reconciled = false;
    @Column(name = "reconciled_by", length = 50)
    private String reconciledBy;
    @Column(name = "reconciled_at")
    private LocalDateTime reconciledAt;
    @Column(name = "reconcile_note", length = 255)
    private String reconcileNote;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
