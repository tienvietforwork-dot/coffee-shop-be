package com.coffeeshop.crm.repository;

import com.coffeeshop.crm.entity.CustomerGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerGroupRepository extends JpaRepository<CustomerGroup, Long> {
    List<CustomerGroup> findAllByGroupTypeAndDelFlagFalse(String groupType);
    List<CustomerGroup> findAllByDelFlagFalseOrderByNameAsc();
    boolean existsByNameIgnoreCaseAndDelFlagFalse(String name);
    boolean existsByNameIgnoreCaseAndDelFlagFalseAndIdNot(String name, Long id);
}
