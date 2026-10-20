package com.coffeeshop.repository;

import com.coffeeshop.entity.GoodsReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, Long> {

    @Query("""
            select r from GoodsReceipt r join fetch r.supplier left join fetch r.purchaseOrder
            where (:supplierId is null or r.supplier.id = :supplierId)
              and (:purchaseOrderId is null or r.purchaseOrder.id = :purchaseOrderId)
            order by r.id desc
            """)
    List<GoodsReceipt> search(Long supplierId, Long purchaseOrderId);

    long countByCodeStartingWith(String prefix);
}
