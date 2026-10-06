package com.coffeeshop.repository;

import com.coffeeshop.entity.Coffee;
import com.coffeeshop.entity.enums.CoffeeStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface CoffeeRepository extends JpaRepository<Coffee, Long> {
    @EntityGraph(attributePaths = "category")
    List<Coffee> findAllByOrderByNameAsc();

    @EntityGraph(attributePaths = "category")
    List<Coffee> findByStatusInOrderByNameAsc(Collection<CoffeeStatus> statuses);

    boolean existsByCategoryId(Long categoryId);
}
