package com.coffeeshop.repository;

import com.coffeeshop.entity.InventorySnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface InventorySnapshotRepository extends JpaRepository<InventorySnapshot, Long> {

    @Query("""
            select s from InventorySnapshot s join fetch s.material
            where s.snapshotDate between :from and :to and (:materialId is null or s.material.id = :materialId)
            order by s.snapshotDate
            """)
    List<InventorySnapshot> findBetween(LocalDate from, LocalDate to, Long materialId);

    @Modifying
    @Query("delete from InventorySnapshot s where s.snapshotDate = :date")
    int deleteByDate(LocalDate date);
}
