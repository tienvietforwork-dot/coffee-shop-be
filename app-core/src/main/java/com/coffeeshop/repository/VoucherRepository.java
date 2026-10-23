package com.coffeeshop.repository;

import com.coffeeshop.entity.Voucher;
import com.coffeeshop.entity.enums.VoucherTypeStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {
    List<Voucher> findByStatusOrderByNameAsc(VoucherTypeStatus status);

    /** Locks the voucher row so two staff cannot take its last free slot at the same time. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Voucher v where v.id = :id")
    Optional<Voucher> lockById(Long id);
}
