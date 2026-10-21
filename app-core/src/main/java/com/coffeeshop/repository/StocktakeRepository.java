package com.coffeeshop.repository;

import com.coffeeshop.entity.Stocktake;
import com.coffeeshop.entity.enums.StocktakeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StocktakeRepository extends JpaRepository<Stocktake, Long> {

    @Query("select s from Stocktake s where (:status is null or s.status = :status) order by s.id desc")
    List<Stocktake> search(StocktakeStatus status);

    long countByCodeStartingWith(String prefix);
}
