package com.coffeeshop.crm.repository;

import com.coffeeshop.crm.entity.CustomerAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, Long> {
    List<CustomerAddress> findAllByCustomerIdAndDelFlagFalseOrderByIdDesc(Long customerId);

    @Query("SELECT a FROM CustomerAddress a WHERE a.customerId = :customerId AND a.isDefault = true AND a.delFlag = false")
    Optional<CustomerAddress> findDefaultByCustomerId(@Param("customerId") Long customerId);

    @Modifying
    @Query("update CustomerAddress a set a.isDefault = false where a.customerId = :customerId and a.delFlag = false")
    int clearAllDefaults(@Param("customerId") Long customerId);

    @Modifying
    @Query("update CustomerAddress a set a.isDefault = false where a.customerId = :customerId and a.delFlag = false and a.id <> :exceptId")
    int clearDefaultExcept(@Param("customerId") Long customerId, @Param("exceptId") Long exceptId);
}
