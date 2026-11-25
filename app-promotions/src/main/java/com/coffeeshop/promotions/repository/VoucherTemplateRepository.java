package com.coffeeshop.promotions.repository;

import com.coffeeshop.promotions.entity.VoucherTemplates;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VoucherTemplateRepository extends JpaRepository<VoucherTemplates, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM VoucherTemplates t WHERE t.id = :id")
    Optional<VoucherTemplates> findByIdForUpdate(@Param("id") Long id);
}
