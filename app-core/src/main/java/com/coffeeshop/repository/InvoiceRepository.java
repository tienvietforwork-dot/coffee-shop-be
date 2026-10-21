package com.coffeeshop.repository;

import com.coffeeshop.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findByOrderIdOrderByIdAsc(Long orderId);

    @Query("""
            select i from Invoice i join fetch i.order o
            where i.createdAt >= :from and i.createdAt < :to and (:status is null or i.status = :status)
              and (:keyword is null or lower(o.orderCode) like :keyword or lower(coalesce(i.invoiceNo, '')) like :keyword
                   or lower(coalesce(i.buyerTaxCode, '')) like :keyword or lower(coalesce(i.buyerCompany, '')) like :keyword)
            order by i.createdAt desc, i.id desc
            """)
    List<Invoice> search(LocalDateTime from, LocalDateTime to, String status, String keyword);

    @Query("select i from Invoice i join fetch i.order where i.status = 'PENDING' and i.attempts < :maxAttempts order by i.id")
    List<Invoice> findQueued(int maxAttempts);

    /** last number used in a series (numbers run on per ký hiệu) */
    @Query("select max(cast(i.invoiceNo as Integer)) from Invoice i where i.series = :series and i.invoiceNo is not null")
    Integer maxNumber(String series);
}
