package com.coffeeshop.repository;

import com.coffeeshop.entity.MaterialTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface MaterialTransactionRepository extends JpaRepository<MaterialTransaction, Long> {
    /** what went into a prepared-material batch */
    @Query("select t from MaterialTransaction t join fetch t.batch b join fetch b.material where t.producedBatch.id = :batchId order by t.id")
    List<MaterialTransaction> findInputsOf(Long batchId);

    @Query("""
            select t from MaterialTransaction t
            join fetch t.batch b join fetch b.material m
            left join fetch t.staff left join fetch t.orderItem
            where (:materialId is null or m.id = :materialId)
            order by t.createdAt desc, t.id desc
            """)
    List<MaterialTransaction> search(Long materialId);

    /**
     * Movements per material and type from {@code from} (inclusive) to {@code to} (exclusive, null = now):
     * [materialId, type, quantity in, quantity out (negative), value in, value out (negative)]; value = quantity × lot cost.
     */
    @Query("""
            select b.material.id, t.type,
                   sum(case when t.quantity > 0 then t.quantity else 0 end),
                   sum(case when t.quantity < 0 then t.quantity else 0 end),
                   sum(case when t.quantity > 0 then t.quantity * coalesce(b.unitCost, 0) else 0 end),
                   sum(case when t.quantity < 0 then t.quantity * coalesce(b.unitCost, 0) else 0 end)
            from MaterialTransaction t join t.batch b
            where t.createdAt >= :from and (cast(:to as timestamp) is null or t.createdAt < :to)
            group by b.material.id, t.type
            """)
    List<Object[]> movements(LocalDateTime from, LocalDateTime to);
}
