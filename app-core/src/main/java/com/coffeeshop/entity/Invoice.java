package com.coffeeshop.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Hóa đơn điện tử khởi tạo từ máy tính tiền (Nghị định 254/2026/NĐ-CP, Thông tư 91/2026/TT-BTC): one per completed order,
 * every line of the order on it; the buyer is optional ("Người mua không lấy hóa đơn" / bán cho người tiêu dùng).
 * Issued through the provider (MISA meInvoice) from a queue. An issued invoice is never deleted or cancelled: mistakes
 * are fixed with a hóa đơn thay thế (REPLACEMENT, the original becomes REPLACED) or điều chỉnh (ADJUSTMENT).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "invoices")
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    /** ORIGINAL, REPLACEMENT or ADJUSTMENT */
    @Column(nullable = false, length = 15)
    private String kind;
    /** the invoice this one replaces / adjusts */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_invoice_id")
    private Invoice related;
    /** PENDING (chờ phát hành), ISSUED, FAILED, REPLACED */
    @Column(nullable = false, length = 15)
    private String status;
    /** mẫu số: 1 = hóa đơn GTGT, 2 = hóa đơn bán hàng */
    @Column(name = "template_code", nullable = false, length = 2)
    private String templateCode;
    /** ký hiệu, ký tự thứ 5 là M (khởi tạo từ máy tính tiền), e.g. 1C26MCH */
    @Column(nullable = false, length = 10)
    private String series;
    @Column(name = "invoice_no", length = 10)
    private String invoiceNo;
    /** mã của cơ quan thuế */
    @Column(name = "tax_authority_code", length = 40)
    private String taxAuthorityCode;
    /** mã tra cứu on the provider's lookup site */
    @Column(name = "lookup_code", length = 40)
    private String lookupCode;
    @Column(name = "lookup_url", length = 255)
    private String lookupUrl;
    @Column(name = "provider_invoice_id", length = 60)
    private String providerInvoiceId;
    // ---- người mua (optional) ----
    @Column(name = "buyer_name", length = 150)
    private String buyerName;
    @Column(name = "buyer_company", length = 255)
    private String buyerCompany;
    @Column(name = "buyer_tax_code", length = 20)
    private String buyerTaxCode;
    @Column(name = "buyer_address", length = 255)
    private String buyerAddress;
    @Column(name = "buyer_email", length = 100)
    private String buyerEmail;
    @Column(name = "buyer_phone", length = 15)
    private String buyerPhone;
    /** số định danh cá nhân (CCCD) */
    @Column(name = "buyer_id_number", length = 20)
    private String buyerIdNumber;
    // ---- tiền ----
    /** tiền hàng chưa thuế */
    @Column(name = "amount_before_tax", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountBeforeTax;
    /** null: hóa đơn bán hàng (không thuế GTGT) */
    @Column(name = "vat_rate", precision = 5, scale = 2)
    private BigDecimal vatRate;
    @Column(name = "vat_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal vatAmount;
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;
    @Column(name = "payment_method", length = 30)
    private String paymentMethod;
    /** replace / adjust: why */
    @Column(length = 255)
    private String reason;
    @Builder.Default
    @Column(nullable = false)
    private int attempts = 0;
    @Column(name = "last_error", length = 500)
    private String lastError;
    @Column(name = "issued_at")
    private LocalDateTime issuedAt;
    @Column(name = "created_by", length = 50)
    private String createdBy;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Builder.Default
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL)
    @OrderBy("lineNo")
    private List<InvoiceLine> lines = new ArrayList<>();
}
