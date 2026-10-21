package com.coffeeshop.repository;

import com.coffeeshop.entity.CashShift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CashShiftRepository extends JpaRepository<CashShift, Long> {

    Optional<CashShift> findFirstByClosedAtIsNullOrderByIdDesc();

    Optional<CashShift> findFirstByClosedAtIsNotNullOrderByClosedAtDesc();

    @Query("select s from CashShift s where s.openedAt >= :from and s.openedAt < :to order by s.openedAt desc")
    List<CashShift> search(LocalDateTime from, LocalDateTime to);

    long countByCodeStartingWith(String prefix);
}
