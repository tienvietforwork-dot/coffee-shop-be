package com.coffeeshop.repository;

import com.coffeeshop.entity.SupplierPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface SupplierPaymentRepository extends JpaRepository<SupplierPayment, Long> {

    @Query("""
            select p from SupplierPayment p join fetch p.supplier
            left join fetch p.goodsReceipt left join fetch p.purchaseOrder
            where (:supplierId is null or p.supplier.id = :supplierId)
              and (cast(:from as timestamp) is null or p.paidAt >= :from)
              and (cast(:to as timestamp) is null or p.paidAt < :to)
            order by p.paidAt desc, p.id desc
            """)
    List<SupplierPayment> search(Long supplierId, LocalDateTime from, LocalDateTime to);

    List<SupplierPayment> findBySupplierIdOrderByPaidAtAscIdAsc(Long supplierId);

    long countByCodeStartingWith(String prefix);
}
