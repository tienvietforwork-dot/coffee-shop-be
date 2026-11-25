package com.coffeeshop.promotions.repository;

import com.coffeeshop.promotions.entity.BaseEntity;
import com.coffeeshop.promotions.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    @Query(value = """
            SELECT u.customer_id
            FROM users u
            WHERE u.username = :username
              AND u.del_flag = FALSE
              AND u.customer_id IS NOT NULL
            """, nativeQuery = true)
    Optional<Long> findCustomerIdByUsername(@Param("username") String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Customer c WHERE c.id = :id")
    Optional<Customer> findByIdForUpdate(@Param("id") Long id);
}
