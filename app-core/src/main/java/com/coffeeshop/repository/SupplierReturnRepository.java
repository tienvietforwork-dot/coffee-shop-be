package com.coffeeshop.repository;

import com.coffeeshop.entity.SupplierReturn;
import com.coffeeshop.entity.enums.SupplierReturnStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface SupplierReturnRepository extends JpaRepository<SupplierReturn, Long> {

    @Query("""
            select r from SupplierReturn r join fetch r.supplier join fetch r.goodsReceipt
            where (:supplierId is null or r.supplier.id = :supplierId)
              and (:status is null or r.status = :status)
              and (:receiptId is null or r.goodsReceipt.id = :receiptId)
            order by r.id desc
            """)
    List<SupplierReturn> search(Long supplierId, SupplierReturnStatus status, Long receiptId);

    List<SupplierReturn> findBySupplierIdAndStatusOrderByCompletedAtAscIdAsc(Long supplierId, SupplierReturnStatus status);

    /** quantity of a receipt line already sent back (completed returns) or held on another open draft */
    @Query("""
            select coalesce(sum(i.quantity), 0) from SupplierReturnItem i
            where i.goodsReceiptItem.id = :receiptItemId and i.supplierReturn.status = :status
              and (:excludeReturnId is null or i.supplierReturn.id <> :excludeReturnId)
            """)
    BigDecimal sumQuantity(Long receiptItemId, SupplierReturnStatus status, Long excludeReturnId);

    long countByCodeStartingWith(String prefix);
}
