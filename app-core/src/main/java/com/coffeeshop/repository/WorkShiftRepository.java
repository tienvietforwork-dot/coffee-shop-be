package com.coffeeshop.repository;

import com.coffeeshop.entity.WorkShift;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkShiftRepository extends JpaRepository<WorkShift, Long> {
    List<WorkShift> findAllByOrderByStartTimeAsc();
}
