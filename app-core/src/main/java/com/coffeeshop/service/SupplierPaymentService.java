package com.coffeeshop.service;

import com.coffeeshop.dto.request.SupplierPaymentRequest;
import com.coffeeshop.dto.response.SupplierPaymentResponse;
import com.coffeeshop.entity.GoodsReceipt;
import com.coffeeshop.entity.PurchaseOrder;
import com.coffeeshop.entity.Supplier;
import com.coffeeshop.entity.SupplierPayment;
import com.coffeeshop.entity.enums.GoodsReceiptStatus;
import com.coffeeshop.entity.enums.PaymentMethod;
import com.coffeeshop.entity.enums.PurchaseOrderStatus;
import com.coffeeshop.entity.enums.SupplierPaymentStatus;
import com.coffeeshop.exception.BadRequestException;
import com.coffeeshop.exception.ResourceNotFoundException;
import com.coffeeshop.repository.GoodsReceiptRepository;
import com.coffeeshop.repository.PurchaseOrderRepository;
import com.coffeeshop.repository.SupplierPaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Phiếu chi trả nhà cung cấp (tiền mặt / chuyển khoản). Not edited; a wrong one is cancelled with a reason. */
@Service
@RequiredArgsConstructor
public class SupplierPaymentService {

    private final SupplierPaymentRepository paymentRepository;
    private final GoodsReceiptRepository receiptRepository;
    private final PurchaseOrderRepository orderRepository;
    private final SupplierService supplierService;
    private final StaffService staffService;
    private final SupplierDebtService debtService;

    @Transactional(readOnly = true)
    public List<SupplierPaymentResponse> search(Long supplierId, LocalDate from, LocalDate to) {
        return paymentRepository.search(supplierId, from == null ? null : from.atStartOfDay(),
                to == null ? null : to.plusDays(1).atStartOfDay()).stream().map(SupplierPaymentResponse::from).toList();
    }

    @Transactional
    public SupplierPaymentResponse create(SupplierPaymentRequest request) {
        Supplier supplier = supplierService.get(request.supplierId());
        GoodsReceipt receipt = null;
        PurchaseOrder order = null;
        if (request.goodsReceiptId() != null) {
            receipt = receiptRepository.findById(request.goodsReceiptId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu nhập: " + request.goodsReceiptId()));
            if (!receipt.getSupplier().getId().equals(supplier.getId())) throw new BadRequestException("Phiếu nhập của nhà cung cấp khác");
            if (receipt.getStatus() == GoodsReceiptStatus.CANCELLED) throw new BadRequestException("Phiếu nhập đã hủy");
        } else if (request.purchaseOrderId() != null) {
            order = orderRepository.findById(request.purchaseOrderId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu đặt: " + request.purchaseOrderId()));
            if (!order.getSupplier().getId().equals(supplier.getId())) throw new BadRequestException("Phiếu đặt của nhà cung cấp khác");
            if (order.getStatus() == PurchaseOrderStatus.DRAFT || order.getStatus() == PurchaseOrderStatus.CANCELLED) {
                throw new BadRequestException("Chỉ trả trước cho phiếu đặt đã gửi nhà cung cấp");
            }
        }
        // trả theo phiếu nhập: không vượt số còn nợ của phiếu; chi chung: không vượt tổng nợ (trả trước thì gắn phiếu đặt)
        if (receipt != null) {
            var s = debtService.settlement(receipt);
            BigDecimal remaining = receipt.payable().subtract(s.paid()).subtract(s.returned()).max(BigDecimal.ZERO);
            if (request.amount().compareTo(remaining) > 0) {
                throw new BadRequestException("Phiếu nhập " + receipt.getCode() + " chỉ còn nợ " + vnd(remaining));
            }
        } else if (order == null) {
            BigDecimal balance = debtService.summary(supplier.getId()).balance().max(BigDecimal.ZERO);
            if (request.amount().compareTo(balance) > 0) {
                throw new BadRequestException("Chỉ còn nợ nhà cung cấp " + vnd(balance) + "; trả trước thì chọn phiếu đặt hàng");
            }
        }
        LocalDateTime paidAt = request.paidAt() == null ? LocalDateTime.now() : request.paidAt();
        if (paidAt.isAfter(LocalDateTime.now().plusMinutes(5))) throw new BadRequestException("Ngày chi ở tương lai");
        if (request.method() != PaymentMethod.CASH && request.method() != PaymentMethod.BANK_TRANSFER) {
            throw new BadRequestException("Trả nhà cung cấp bằng tiền mặt hoặc chuyển khoản");
        }
        SupplierPayment payment = paymentRepository.save(SupplierPayment.builder()
                .code(newCode()).supplier(supplier).goodsReceipt(receipt).purchaseOrder(order)
                .amount(request.amount()).method(request.method()).reference(blankToNull(request.reference()))
                .payeeName(blankToNull(request.payeeName())).paidAt(paidAt).staff(staffService.current())
                .documentImageId(request.documentImageId()).note(blankToNull(request.note())).build());
        return SupplierPaymentResponse.from(payment);
    }

    /** Hủy phiếu chi (chi nhầm, nhập sai số tiền): it no longer counts against the debt. */
    @Transactional
    public SupplierPaymentResponse cancel(Long id, String reason) {
        SupplierPayment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu chi: " + id));
        if (payment.getStatus() == SupplierPaymentStatus.CANCELLED) throw new BadRequestException("Phiếu chi đã hủy");
        payment.setStatus(SupplierPaymentStatus.CANCELLED);
        payment.setCancelReason(reason.trim());
        return SupplierPaymentResponse.from(payment);
    }

    /** Paid on the spot when the goods arrived. */
    void recordOnReceipt(GoodsReceipt receipt, BigDecimal amount, PaymentMethod method, String reference) {
        if (method != PaymentMethod.CASH && method != PaymentMethod.BANK_TRANSFER) {
            throw new BadRequestException("Trả nhà cung cấp bằng tiền mặt hoặc chuyển khoản");
        }
        paymentRepository.save(SupplierPayment.builder()
                .code(newCode()).supplier(receipt.getSupplier()).goodsReceipt(receipt).amount(amount).method(method)
                .reference(blankToNull(reference)).payeeName(receipt.getDelivererName()).paidAt(LocalDateTime.now())
                .staff(staffService.current()).note("Trả khi nhận hàng " + receipt.getCode()).build());
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    /** PC + yyMMdd + running number of the day: PC261020-001 */
    private String newCode() {
        String prefix = "PC" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd")) + "-";
        return prefix + String.format("%03d", paymentRepository.countByCodeStartingWith(prefix) + 1);
    }

    private static String vnd(BigDecimal v) {
        return String.format("%,.0f", v).replace(',', '.') + "đ";
    }
}
