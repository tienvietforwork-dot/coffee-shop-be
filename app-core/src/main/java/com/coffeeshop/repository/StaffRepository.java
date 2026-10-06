package com.coffeeshop.repository;

import com.coffeeshop.entity.Staff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StaffRepository extends JpaRepository<Staff, Long> {
    List<Staff> findAllByOrderByFullNameAsc();

    boolean existsByPhone(String phone);
}
