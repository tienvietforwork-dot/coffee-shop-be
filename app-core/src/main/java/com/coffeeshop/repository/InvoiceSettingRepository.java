package com.coffeeshop.repository;

import com.coffeeshop.entity.InvoiceSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceSettingRepository extends JpaRepository<InvoiceSetting, Long> {
}
