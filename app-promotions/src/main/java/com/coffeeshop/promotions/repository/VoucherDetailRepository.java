package com.coffeeshop.promotions.repository;

import com.coffeeshop.promotions.entity.VoucherDetail;
import com.coffeeshop.promotions.entity.enums.VoucherStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoucherDetailRepository extends JpaRepository<VoucherDetail, Long> {

    long countByStatus(VoucherStatus status);

    List<VoucherDetail> findByStatus(VoucherStatus status);
}
