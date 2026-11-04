package com.coffeeshop.promotions.repository;

import com.coffeeshop.promotions.entity.VoucherDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VoucherDetailRepository extends JpaRepository<VoucherDetail, Long> {
}
