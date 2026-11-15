package com.coffeeshop.repository;

import com.coffeeshop.entity.CustomerAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, Long> {
    List<CustomerAddress> findByCustomerIdAndDelFlagFalseOrderByDefaultAddressDescIdDesc(Long customerId);

    Optional<CustomerAddress> findByIdAndDelFlagFalse(Long id);
}
