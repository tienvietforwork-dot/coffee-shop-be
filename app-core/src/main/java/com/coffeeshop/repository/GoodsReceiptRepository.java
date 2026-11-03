package com.coffeeshop.repository;

import com.coffeeshop.entity.GoodsReceipt;
import com.coffeeshop.entity.enums.GoodsReceiptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, Long> {

    @Query("""
            select r from GoodsReceipt r join fetch r.supplier left join fetch r.purchaseOrder
            where (:supplierId is null or r.supplier.id = :supplierId)
              and (:purchaseOrderId is null or r.purchaseOrder.id = :purchaseOrderId)
              and (:status is null or r.status = :status)
              and (cast(:from as timestamp) is null or r.createdAt >= :from)
              and (cast(:to as timestamp) is null or r.createdAt < :to)
            order by r.id desc
            """)
    List<GoodsReceipt> search(Long supplierId, Long purchaseOrderId, GoodsReceiptStatus status,
                              LocalDateTime from, LocalDateTime to);

    /** a supplier's receipts, oldest first (công nợ is settled in that order) */
    List<GoodsReceipt> findBySupplierIdOrderByCreatedAtAscIdAsc(Long supplierId);

    long countByCodeStartingWith(String prefix);
}
