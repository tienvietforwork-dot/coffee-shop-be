package com.coffeeshop.repository;

import com.coffeeshop.entity.VoucherDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VoucherDetailRepository extends JpaRepository<VoucherDetail, Long> {
    Optional<VoucherDetail> findByCodeIgnoreCase(String code);

    boolean existsByCode(String code);

    List<VoucherDetail> findByIncidentId(Long incidentId);

    List<VoucherDetail> findByOrderId(Long orderId);

    List<VoucherDetail> findByCustomerIdOrderByIssuedAtDesc(Long customerId);

    /** Lượt đã chiếm của một loại voucher: đã dùng + đang giữ chỗ (chưa dùng, còn hạn). */
    @Query("""
            select count(d) from VoucherDetail d where d.voucher.id = :voucherId
              and (d.status = com.coffeeshop.entity.enums.VoucherStatus.USED
                or (d.status = com.coffeeshop.entity.enums.VoucherStatus.ISSUED and (d.expiresAt is null or d.expiresAt > :now)))
            """)
    long countTakenSlots(Long voucherId, LocalDateTime now);
}
