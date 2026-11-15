package com.coffeeshop.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Thiết lập hóa đơn điện tử (1 dòng, id = 1): thông tin người bán đã đăng ký với cơ quan thuế và ký hiệu hóa đơn.
 * Ký hiệu = mẫu số (1 GTGT / 2 bán hàng, theo {@code vatRate}) + C (có mã CQT) + 2 số cuối năm + M (máy tính tiền)
 * + {@code seriesSuffix}. Tài khoản nhà cung cấp (MISA) vẫn để ở biến môi trường, không lưu DB.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "invoice_settings")
public class InvoiceSetting extends BaseEntity {
    public static final long ID = 1L;

    @Id
    private Long id;
    @Column(name = "seller_name", nullable = false, length = 255)
    private String sellerName;
    @Column(name = "seller_tax_code", nullable = false, length = 14)
    private String sellerTaxCode;
    /** null = dùng địa chỉ quán (thiết lập giao hàng) */
    @Column(name = "seller_address", length = 255)
    private String sellerAddress;
    /** null: hóa đơn bán hàng (hộ kinh doanh / phương pháp trực tiếp); có thuế suất: hóa đơn GTGT */
    @Column(name = "vat_rate", precision = 5, scale = 2)
    private BigDecimal vatRate;
    /** 2 chữ cái in hoa người bán tự đặt ở cuối ký hiệu, e.g. CH */
    @Column(name = "series_suffix", nullable = false, length = 2)
    private String seriesSuffix;
    /** 5 ký tự trong mã của CQT (M1-YY-XXXXX-…) cấp khi đăng ký dùng hóa đơn từ máy tính tiền */
    @Column(name = "tax_office_code", length = 5)
    private String taxOfficeCode;
    @Column(name = "lookup_url", length = 255)
    private String lookupUrl;
}
