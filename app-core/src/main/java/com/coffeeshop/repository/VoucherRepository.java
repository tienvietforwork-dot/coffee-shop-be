package com.coffeeshop.repository;

import com.coffeeshop.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {
    Optional<Voucher> findByCodeIgnoreCase(String code);

    boolean existsByCode(String code);

    List<Voucher> findByIncidentId(Long incidentId);

    List<Voucher> findByOrderId(Long orderId);

    List<Voucher> findByCustomerIdOrderByIssuedAtDesc(Long customerId);
}
