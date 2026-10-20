package com.coffeeshop.service;

import com.coffeeshop.dto.request.StockImportRequest;
import com.coffeeshop.dto.response.BatchResponse;
import com.coffeeshop.dto.response.GoodsReceiptResponse;
import com.coffeeshop.entity.GoodsReceipt;
import com.coffeeshop.entity.Material;
import com.coffeeshop.entity.MaterialPackage;
import com.coffeeshop.entity.PurchaseOrder;
import com.coffeeshop.entity.Staff;
import com.coffeeshop.entity.Supplier;
import com.coffeeshop.exception.BadRequestException;
import com.coffeeshop.repository.GoodsReceiptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Phiếu nhập kho: against a purchase order (see {@link PurchaseOrderService#receive}) or loose. Never edited or deleted. */
@Service
@RequiredArgsConstructor
public class GoodsReceiptService {

    private final GoodsReceiptRepository receiptRepository;
    private final SupplierService supplierService;
    private final InventoryService inventoryService;
    private final StaffService staffService;

    @Transactional(readOnly = true)
    public List<GoodsReceiptResponse> search(Long supplierId, Long purchaseOrderId) {
        return receiptRepository.search(supplierId, purchaseOrderId).stream().map(GoodsReceiptResponse::from).toList();
    }

    /** Nhập lẻ, without a purchase order: one lot, the supplier is still required. */
    @Transactional
    public BatchResponse importLoose(StockImportRequest request) {
        Supplier supplier = supplierService.active(request.supplierId());
        Material material = inventoryService.purchasable(request.materialId());
        MaterialPackage pkg = inventoryService.packageOf(material, request.packageId());
        BigDecimal quantity = request.quantity();
        if (pkg != null) {
            if (request.packageCount() == null) throw new BadRequestException("Nhập số " + pkg.getName());
            quantity = pkg.getSize().multiply(request.packageCount()).setScale(2, RoundingMode.HALF_UP);
        }
        if (quantity == null) throw new BadRequestException("Nhập số lượng");
        Staff staff = staffService.current();
        GoodsReceipt receipt = open(supplier, null, staff, request.note());
        return BatchResponse.from(inventoryService.receiveLot(material, quantity,
                pkg == null ? null : pkg.getName(), pkg == null ? null : pkg.getSize(),
                pkg == null ? null : request.packageCount(), request.unitCost(), request.expiryDate(),
                receipt, null, staff, request.note()));
    }

    /** New phiếu nhập header; lines are added with {@link InventoryService#receiveLot}. */
    GoodsReceipt open(Supplier supplier, PurchaseOrder order, Staff staff, String note) {
        return receiptRepository.save(GoodsReceipt.builder()
                .code(newCode()).supplier(supplier).purchaseOrder(order).staff(staff)
                .note(note == null || note.isBlank() ? null : note.trim()).build());
    }

    /** PN + yyMMdd + running number of the day: PN261020-001 */
    private String newCode() {
        String prefix = "PN" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd")) + "-";
        return prefix + String.format("%03d", receiptRepository.countByCodeStartingWith(prefix) + 1);
    }
}
