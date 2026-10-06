package com.coffeeshop.repository;

import com.coffeeshop.entity.MaterialBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface MaterialBatchRepository extends JpaRepository<MaterialBatch, Long> {
    List<MaterialBatch> findByMaterialIdOrderByIdDesc(Long materialId);

    /** FEFO: earliest expiry first, batches without expiry last, then oldest batch first. */
    @Query("""
            select b from MaterialBatch b
            where b.material.id = :materialId
              and b.status = com.coffeeshop.entity.enums.BatchStatus.AVAILABLE
              and b.remainingQuantity > 0
            order by case when b.expiryDate is null then 1 else 0 end, b.expiryDate, b.id
            """)
    List<MaterialBatch> findAvailableFefo(Long materialId);

    @Query("""
            select b from MaterialBatch b join fetch b.material
            where b.status = com.coffeeshop.entity.enums.BatchStatus.AVAILABLE
              and b.remainingQuantity > 0
              and b.expiryDate is not null and b.expiryDate <= :until
            order by b.expiryDate
            """)
    List<MaterialBatch> findExpiringBefore(LocalDate until);
}
