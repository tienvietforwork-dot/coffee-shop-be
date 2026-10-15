package com.coffeeshop.crm.repository;

import com.coffeeshop.crm.entity.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    Page<Feedback> findAllByCustomerIdAndDelFlagFalseOrderByCreatedAtDesc(Long customerId, Pageable pageable);
    Page<Feedback> findAllByDelFlagFalseOrderByCreatedAtDesc(Pageable pageable);

    @Query("select f from Feedback f where f.customerId = :customerId and f.delFlag = false and f.createdAt >= :fromDate order by f.createdAt desc")
    Page<Feedback> findForCustomerSince(@Param("customerId") Long customerId,
                                        @Param("fromDate") java.time.LocalDateTime fromDate,
                                        Pageable pageable);

    @Query("select f from Feedback f where f.customerId = :customerId and f.delFlag = false and f.createdAt < :toDate order by f.createdAt desc")
    Page<Feedback> findForCustomerBefore(@Param("customerId") Long customerId,
                                         @Param("toDate") java.time.LocalDateTime toDate,
                                         Pageable pageable);

    @Query("select f from Feedback f where f.customerId = :customerId and f.delFlag = false and f.createdAt >= :fromDate and f.createdAt < :toDate order by f.createdAt desc")
    Page<Feedback> findForCustomerBetween(@Param("customerId") Long customerId,
                                          @Param("fromDate") java.time.LocalDateTime fromDate,
                                          @Param("toDate") java.time.LocalDateTime toDate,
                                          Pageable pageable);

    @Modifying
    @Query("update Feedback f set f.customerId = :targetId where f.customerId = :sourceId and f.delFlag = false")
    int updateCustomerId(@Param("sourceId") Long sourceId, @Param("targetId") Long targetId);
}
