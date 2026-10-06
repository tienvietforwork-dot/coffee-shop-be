package com.coffeeshop.repository;

import com.coffeeshop.entity.MaterialTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MaterialTransactionRepository extends JpaRepository<MaterialTransaction, Long> {
    @Query("""
            select t from MaterialTransaction t
            join fetch t.batch b join fetch b.material m
            left join fetch t.staff left join fetch t.orderItem
            where (:materialId is null or m.id = :materialId)
            order by t.createdAt desc, t.id desc
            """)
    List<MaterialTransaction> search(Long materialId);
}
