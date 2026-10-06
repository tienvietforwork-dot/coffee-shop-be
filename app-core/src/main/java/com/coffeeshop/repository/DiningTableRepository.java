package com.coffeeshop.repository;

import com.coffeeshop.entity.DiningTable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DiningTableRepository extends JpaRepository<DiningTable, Long> {
    List<DiningTable> findAllByOrderByTableNoAsc();

    Optional<DiningTable> findByQrCode(String qrCode);

    boolean existsByTableNo(String tableNo);
}
