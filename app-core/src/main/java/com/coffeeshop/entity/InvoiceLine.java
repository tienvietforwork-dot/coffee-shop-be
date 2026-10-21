package com.coffeeshop.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Dòng hóa đơn: each drink separately (never one lumped line), then chiết khấu (negative) and phí giao hàng.
 * Prices as sold (VAT included when the invoice carries VAT).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "invoice_lines")
public class InvoiceLine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;
    @Column(name = "line_no", nullable = false)
    private int lineNo;
    @Column(nullable = false, length = 255)
    private String name;
    @Column(nullable = false, length = 20)
    private String unit;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal quantity;
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;
}
