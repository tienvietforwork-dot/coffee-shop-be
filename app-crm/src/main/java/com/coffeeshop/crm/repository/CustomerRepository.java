package com.coffeeshop.crm.repository;

import com.coffeeshop.crm.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Page<Customer> findAllByDelFlagFalseOrderByIdDesc(Pageable pageable);

    @Query("select c from Customer c where c.delFlag = false and " +
            "(lower(coalesce(c.fullName, '')) like lower(concat('%', :query, '%')) " +
            "or lower(c.phone) like lower(concat('%', :query, '%')) " +
            "or lower(coalesce(c.email, '')) like lower(concat('%', :query, '%'))) " +
            "order by c.id desc")
    Page<Customer> search(@Param("query") String query, Pageable pageable);

    boolean existsByPhoneAndDelFlagFalse(String phone);

    Optional<Customer> findByPhoneAndDelFlagFalse(String phone);
}
