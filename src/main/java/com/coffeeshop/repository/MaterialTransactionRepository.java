package com.coffeeshop.repository;

import com.coffeeshop.entity.MaterialTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaterialTransactionRepository extends JpaRepository<MaterialTransaction, Long> {
    List<MaterialTransaction> findByMaterialIdOrderByCreatedAtDesc(Long materialId);
}
