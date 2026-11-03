package com.coffeeshop.service;

import com.coffeeshop.dto.response.GoodsReceiptResponse;
import com.coffeeshop.dto.response.SupplierDebtResponse;
import com.coffeeshop.dto.response.SupplierLedgerResponse;
import com.coffeeshop.entity.GoodsReceipt;
import com.coffeeshop.entity.Supplier;
import com.coffeeshop.entity.SupplierPayment;
import com.coffeeshop.entity.SupplierReturn;
import com.coffeeshop.entity.enums.GoodsReceiptStatus;
import com.coffeeshop.entity.enums.SupplierPaymentStatus;
import com.coffeeshop.entity.enums.SupplierReturnSettlement;
import com.coffeeshop.entity.enums.SupplierReturnStatus;
import com.coffeeshop.exception.ResourceNotFoundException;
import com.coffeeshop.repository.GoodsReceiptRepository;
import com.coffeeshop.repository.SupplierPaymentRepository;
import com.coffeeshop.repository.SupplierRepository;
import com.coffeeshop.repository.SupplierReturnRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Công nợ phải trả nhà cung cấp, computed from the documents (nothing is stored twice):
 * phiếu nhập raise it, phiếu trả hàng settled as "trừ công nợ" and phiếu chi lower it.
 * <p>
 * Per receipt: its own returns are credited to it; a payment made on a receipt goes to that receipt first;
 * everything else pays the oldest open receipts first (FIFO). What is left over is trả trước.
 */
@Service
@RequiredArgsConstructor
public class SupplierDebtService {

    private final SupplierRepository supplierRepository;
    private final GoodsReceiptRepository receiptRepository;
    private final SupplierPaymentRepository paymentRepository;
    private final SupplierReturnRepository returnRepository;

    /** How a supplier's open documents settle each other. */
    record Book(List<GoodsReceipt> receipts, List<SupplierReturn> returns, List<SupplierPayment> payments,
                Map<Long, GoodsReceiptResponse.Settlement> settlements, BigDecimal advance) {
    }

    @Transactional(readOnly = true)
    public List<SupplierDebtResponse> summary() {
        return supplierRepository.findAllByOrderByNameAsc().stream().map(s -> summarize(s, book(s.getId()))).toList();
    }

    @Transactional(readOnly = true)
    public SupplierDebtResponse summary(Long supplierId) {
        return summarize(supplier(supplierId), book(supplierId));
    }

    /** Settlement of every receipt of the supplier, by receipt id. */
    Map<Long, GoodsReceiptResponse.Settlement> settlements(Long supplierId) {
        return book(supplierId).settlements();
    }

    GoodsReceiptResponse.Settlement settlement(GoodsReceipt receipt) {
        return settlements(receipt.getSupplier().getId()).getOrDefault(receipt.getId(), GoodsReceiptResponse.Settlement.NONE);
    }

    @Transactional(readOnly = true)
    public SupplierLedgerResponse ledger(Long supplierId, LocalDate from, LocalDate to) {
        Supplier supplier = supplier(supplierId);
        Book book = book(supplierId);
        List<SupplierLedgerResponse.Row> events = new ArrayList<>();
        for (GoodsReceipt r : book.receipts()) {
            String desc = "Nhập hàng" + (r.getPurchaseOrder() == null ? "" : " theo " + r.getPurchaseOrder().getCode())
                    + (r.getInvoiceNo() == null ? "" : " · HĐ " + nz(r.getInvoiceSeries()) + r.getInvoiceNo());
            events.add(new SupplierLedgerResponse.Row(r.getCreatedAt(), "RECEIPT", r.getId(), r.getCode(), desc,
                    r.payable(), BigDecimal.ZERO, null, r.getDueDate()));
        }
        for (SupplierReturn r : book.returns()) {
            if (r.getSettlement() != SupplierReturnSettlement.DEBT) continue;
            events.add(new SupplierLedgerResponse.Row(r.getCompletedAt(), "RETURN", r.getId(), r.getCode(),
                    "Trả hàng phiếu " + r.getGoodsReceipt().getCode() + " · " + r.getReason(),
                    BigDecimal.ZERO, r.totalAmount(), null, null));
        }
        for (SupplierPayment p : book.payments()) {
            String desc = "Chi trả " + (p.getMethod() == com.coffeeshop.entity.enums.PaymentMethod.CASH ? "tiền mặt" : "chuyển khoản")
                    + (p.getGoodsReceipt() != null ? " · phiếu " + p.getGoodsReceipt().getCode()
                    : p.getPurchaseOrder() != null ? " · trả trước " + p.getPurchaseOrder().getCode() : "");
            events.add(new SupplierLedgerResponse.Row(p.getPaidAt(), "PAYMENT", p.getId(), p.getCode(), desc,
                    BigDecimal.ZERO, p.getAmount(), null, null));
        }
        events.sort(Comparator.comparing(SupplierLedgerResponse.Row::date).thenComparing(SupplierLedgerResponse.Row::code));

        LocalDateTime start = from == null ? null : from.atStartOfDay();
        LocalDateTime end = to == null ? null : to.plusDays(1).atStartOfDay();
        BigDecimal opening = BigDecimal.ZERO, increase = BigDecimal.ZERO, decrease = BigDecimal.ZERO;
        List<SupplierLedgerResponse.Row> rows = new ArrayList<>();
        for (var e : events) {
            if (start != null && e.date().isBefore(start)) {
                opening = opening.add(e.increase()).subtract(e.decrease());
                continue;
            }
            if (end != null && !e.date().isBefore(end)) continue;
            increase = increase.add(e.increase());
            decrease = decrease.add(e.decrease());
            BigDecimal balance = opening.add(increase).subtract(decrease);
            rows.add(new SupplierLedgerResponse.Row(e.date(), e.type(), e.documentId(), e.code(), e.description(),
                    e.increase(), e.decrease(), balance, e.dueDate()));
        }
        return new SupplierLedgerResponse(supplier.getId(), supplier.getName(), supplier.getTaxCode(),
                supplier.getAddress(), from, to, opening, increase, decrease, opening.add(increase).subtract(decrease), rows);
    }

    // ---- computation -----------------------------------------------------

    Book book(Long supplierId) {
        List<GoodsReceipt> receipts = receiptRepository.findBySupplierIdOrderByCreatedAtAscIdAsc(supplierId).stream()
                .filter(r -> r.getStatus() == GoodsReceiptStatus.COMPLETED).toList();
        List<SupplierReturn> returns = returnRepository
                .findBySupplierIdAndStatusOrderByCompletedAtAscIdAsc(supplierId, SupplierReturnStatus.COMPLETED);
        List<SupplierPayment> payments = paymentRepository.findBySupplierIdOrderByPaidAtAscIdAsc(supplierId).stream()
                .filter(p -> p.getStatus() == SupplierPaymentStatus.COMPLETED).toList();

        Map<Long, BigDecimal> open = new LinkedHashMap<>();
        Map<Long, BigDecimal> returned = new HashMap<>();
        Map<Long, BigDecimal> paid = new HashMap<>();
        BigDecimal pool = BigDecimal.ZERO;
        for (GoodsReceipt r : receipts) open.put(r.getId(), r.payable());
        for (SupplierReturn ret : returns) {
            if (ret.getSettlement() != SupplierReturnSettlement.DEBT) continue;
            Long id = ret.getGoodsReceipt().getId();
            BigDecimal left = open.getOrDefault(id, BigDecimal.ZERO);
            BigDecimal take = ret.totalAmount().min(left);
            returned.merge(id, take, BigDecimal::add);
            if (open.containsKey(id)) open.put(id, left.subtract(take));
            pool = pool.add(ret.totalAmount().subtract(take));
        }
        for (SupplierPayment p : payments) {
            BigDecimal amount = p.getAmount();
            if (p.getGoodsReceipt() != null && open.containsKey(p.getGoodsReceipt().getId())) {
                Long id = p.getGoodsReceipt().getId();
                BigDecimal take = amount.min(open.get(id));
                paid.merge(id, take, BigDecimal::add);
                open.put(id, open.get(id).subtract(take));
                amount = amount.subtract(take);
            }
            pool = pool.add(amount);
        }
        for (var e : open.entrySet()) {
            if (pool.signum() <= 0) break;
            BigDecimal take = pool.min(e.getValue());
            if (take.signum() <= 0) continue;
            paid.merge(e.getKey(), take, BigDecimal::add);
            e.setValue(e.getValue().subtract(take));
            pool = pool.subtract(take);
        }
        Map<Long, GoodsReceiptResponse.Settlement> settlements = new HashMap<>();
        for (GoodsReceipt r : receipts) {
            settlements.put(r.getId(), new GoodsReceiptResponse.Settlement(
                    paid.getOrDefault(r.getId(), BigDecimal.ZERO), returned.getOrDefault(r.getId(), BigDecimal.ZERO)));
        }
        return new Book(receipts, returns, payments, settlements, pool);
    }

    private SupplierDebtResponse summarize(Supplier s, Book book) {
        BigDecimal purchased = book.receipts().stream().map(GoodsReceipt::payable).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal returned = book.returns().stream().filter(r -> r.getSettlement() == SupplierReturnSettlement.DEBT)
                .map(SupplierReturn::totalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal paid = book.payments().stream().map(SupplierPayment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal overdue = BigDecimal.ZERO;
        int openCount = 0;
        LocalDate nextDue = null;
        LocalDate today = LocalDate.now();
        for (GoodsReceipt r : book.receipts()) {
            var st = book.settlements().get(r.getId());
            BigDecimal remaining = r.payable().subtract(st.paid()).subtract(st.returned());
            if (remaining.signum() <= 0) continue;
            openCount++;
            if (r.getDueDate().isBefore(today)) overdue = overdue.add(remaining);
            else if (nextDue == null || r.getDueDate().isBefore(nextDue)) nextDue = r.getDueDate();
        }
        return new SupplierDebtResponse(s.getId(), s.getName(), s.getPhone(), s.getPaymentTermDays(), purchased,
                returned, paid, purchased.subtract(returned).subtract(paid), overdue, openCount, nextDue);
    }

    private Supplier supplier(Long id) {
        return supplierRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhà cung cấp: " + id));
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
