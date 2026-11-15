package com.coffeeshop.promotions.repository;

import com.coffeeshop.promotions.entity.BaseEntity;
import com.coffeeshop.promotions.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
}
