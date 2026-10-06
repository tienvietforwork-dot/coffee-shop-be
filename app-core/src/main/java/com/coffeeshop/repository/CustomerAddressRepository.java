package com.coffeeshop.repository;

import com.coffeeshop.entity.CustomerAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, Long> {
    List<CustomerAddress> findByCustomerIdOrderByDefaultAddressDescIdDesc(Long customerId);
}
