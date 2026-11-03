package com.coffeeshop.repository;

import com.coffeeshop.entity.CoffeeCostSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CoffeeCostSnapshotRepository extends JpaRepository<CoffeeCostSnapshot, Long> {

    Optional<CoffeeCostSnapshot> findFirstByCoffeeIdOrderByComputedAtDescIdDesc(Long coffeeId);

    List<CoffeeCostSnapshot> findByCoffeeIdOrderByComputedAtAsc(Long coffeeId);
}
