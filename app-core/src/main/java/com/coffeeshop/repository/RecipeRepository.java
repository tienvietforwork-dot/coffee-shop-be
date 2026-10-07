package com.coffeeshop.repository;

import com.coffeeshop.entity.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    List<Recipe> findByCoffeeIdOrderByVersionDesc(Long coffeeId);

    Optional<Recipe> findByCoffeeIdAndActiveTrue(Long coffeeId);

    /** Recipes in use for coffees that still exist (the join applies Coffee's del_flag restriction). */
    @Query("select r from Recipe r join fetch r.coffee where r.active = true")
    List<Recipe> findAllActive();

    /** Active recipes with their materials (and stock) in one query — for stock-based availability. */
    @Query("select distinct r from Recipe r join fetch r.coffee left join fetch r.materials rm left join fetch rm.material where r.active = true")
    List<Recipe> findAllActiveWithMaterials();

    @Query("select coalesce(max(r.version), 0) from Recipe r where r.coffee.id = :coffeeId")
    int maxVersion(Long coffeeId);
}
