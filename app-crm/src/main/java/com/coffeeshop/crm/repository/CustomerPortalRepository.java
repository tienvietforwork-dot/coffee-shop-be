package com.coffeeshop.crm.repository;

import com.coffeeshop.crm.entity.Customer;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/** Reads the shared Core account-to-customer link for the authenticated user. */
public interface CustomerPortalRepository extends Repository<Customer, Long> {
    @Query(value = "select customer_id from users where id = :userId and active = true and del_flag = false",
            nativeQuery = true)
    Optional<Long> findActiveCustomerIdByUserId(@Param("userId") Long userId);
}
