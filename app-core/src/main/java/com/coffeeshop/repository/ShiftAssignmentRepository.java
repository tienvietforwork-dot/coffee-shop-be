package com.coffeeshop.repository;

import com.coffeeshop.entity.ShiftAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface ShiftAssignmentRepository extends JpaRepository<ShiftAssignment, Long> {

    @Query("""
            select a from ShiftAssignment a join fetch a.staff join fetch a.workShift
            where a.workDate between :from and :to and (:staffId is null or a.staff.id = :staffId)
            order by a.workDate, a.workShift.startTime, a.staff.fullName
            """)
    List<ShiftAssignment> findBetween(LocalDate from, LocalDate to, Long staffId);

    boolean existsByStaffIdAndWorkDateAndWorkShiftId(Long staffId, LocalDate workDate, Long workShiftId);

    boolean existsByWorkShiftIdAndWorkDateGreaterThanEqual(Long workShiftId, LocalDate from);
}
