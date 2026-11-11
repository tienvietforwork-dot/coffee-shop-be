package com.coffeeshop.dto.response;

import com.coffeeshop.entity.Invoice;
import com.coffeeshop.entity.InvoiceLine;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Hóa đơn điện tử, with the seller block for printing. */
public record InvoiceResponse(Long id, Long orderId, String orderCode, String kind, Long relatedId, String relatedNo,
                              String status, String templateCode, String series, String invoiceNo, String taxAuthorityCode,
                              String lookupCode, String lookupUrl, String sellerName, String sellerTaxCode,
                              String sellerAddress, String buyerName, String buyerCompany, String buyerTaxCode,
                              String buyerAddress, String buyerEmail, String buyerPhone, String buyerIdNumber,
                              BigDecimal amountBeforeTax, BigDecimal vatRate, BigDecimal vatAmount, BigDecimal totalAmount,
                              String paymentMethod, String reason, int attempts, String lastError, LocalDateTime issuedAt,
                              LocalDateTime createdAt, List<Line> lines) {

    public record Line(int lineNo, String name, String unit, BigDecimal quantity, BigDecimal unitPrice, BigDecimal amount) {
        static Line from(InvoiceLine l) {
            return new Line(l.getLineNo(), l.getName(), l.getUnit(), l.getQuantity(), l.getUnitPrice(), l.getAmount());
        }
    }

    public static InvoiceResponse from(Invoice i, String sellerName, String sellerTaxCode, String sellerAddress) {
        var r = i.getRelated();
        return new InvoiceResponse(i.getId(), i.getOrder().getId(), i.getOrder().getOrderCode(), i.getKind(),
                r == null ? null : r.getId(), r == null ? null : r.getInvoiceNo(), i.getStatus(), i.getTemplateCode(),
                i.getSeries(), i.getInvoiceNo(), i.getTaxAuthorityCode(), i.getLookupCode(), i.getLookupUrl(), sellerName,
                sellerTaxCode, sellerAddress, i.getBuyerName(), i.getBuyerCompany(), i.getBuyerTaxCode(), i.getBuyerAddress(),
                i.getBuyerEmail(), i.getBuyerPhone(), i.getBuyerIdNumber(), i.getAmountBeforeTax(), i.getVatRate(),
                i.getVatAmount(), i.getTotalAmount(), i.getPaymentMethod(), i.getReason(), i.getAttempts(), i.getLastError(),
                i.getIssuedAt(), i.getCreatedAt(), i.getLines().stream().map(Line::from).toList());
    }
}
