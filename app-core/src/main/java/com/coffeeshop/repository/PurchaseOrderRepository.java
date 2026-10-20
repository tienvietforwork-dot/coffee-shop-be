package com.coffeeshop.repository;

import com.coffeeshop.entity.PurchaseOrder;
import com.coffeeshop.entity.enums.PurchaseOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    @Query("""
            select p from PurchaseOrder p join fetch p.supplier
            where (:status is null or p.status = :status)
              and (:supplierId is null or p.supplier.id = :supplierId)
            order by p.id desc
            """)
    List<PurchaseOrder> search(PurchaseOrderStatus status, Long supplierId);

    long countByCodeStartingWith(String prefix);
}
