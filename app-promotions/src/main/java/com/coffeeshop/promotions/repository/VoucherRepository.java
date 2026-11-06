package com.coffeeshop.promotions.repository;

import com.coffeeshop.promotions.entity.Voucher;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long> {



    long countByNameStartingWith(String name);
}
