package com.coffeeshop.repository;

import com.coffeeshop.entity.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    List<Recipe> findByCoffeeIdOrderByVersionDesc(Long coffeeId);

    Optional<Recipe> findByCoffeeIdAndActiveTrue(Long coffeeId);

    @Query("select coalesce(max(r.version), 0) from Recipe r where r.coffee.id = :coffeeId")
    int maxVersion(Long coffeeId);
}
