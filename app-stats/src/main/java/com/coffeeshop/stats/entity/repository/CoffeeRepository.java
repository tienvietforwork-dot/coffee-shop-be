package com.coffeeshop.stats.entity.repository;

import com.coffeeshop.stats.entity.Coffee;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CoffeeRepository extends JpaRepository<Coffee, Long> {

    @EntityGraph(attributePaths = "category")
    List<Coffee> findAllByOrderByNameAsc();
}
