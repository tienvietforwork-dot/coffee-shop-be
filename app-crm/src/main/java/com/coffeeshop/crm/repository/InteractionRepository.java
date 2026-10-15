package com.coffeeshop.crm.repository;

import com.coffeeshop.crm.entity.Interaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InteractionRepository extends JpaRepository<Interaction, Long> {
    Page<Interaction> findAllByCustomerIdAndDelFlagFalseOrderByOccurredAtDesc(Long customerId, Pageable pageable);
    Page<Interaction> findAllByCustomerIdAndVisibleToCustomerTrueAndDelFlagFalseOrderByOccurredAtDesc(Long customerId, Pageable pageable);
    Page<Interaction> findAllByDelFlagFalseOrderByOccurredAtDesc(Pageable pageable);

    @Query("select i from Interaction i where i.customerId = :customerId and i.delFlag = false and i.occurredAt >= :fromDate order by i.occurredAt desc")
    Page<Interaction> findForCustomerSince(@Param("customerId") Long customerId,
                                           @Param("fromDate") java.time.LocalDateTime fromDate,
                                           Pageable pageable);

    @Query("select i from Interaction i where i.customerId = :customerId and i.delFlag = false and i.occurredAt < :toDate order by i.occurredAt desc")
    Page<Interaction> findForCustomerBefore(@Param("customerId") Long customerId,
                                            @Param("toDate") java.time.LocalDateTime toDate,
                                            Pageable pageable);

    @Query("select i from Interaction i where i.customerId = :customerId and i.delFlag = false and i.occurredAt >= :fromDate and i.occurredAt < :toDate order by i.occurredAt desc")
    Page<Interaction> findForCustomerBetween(@Param("customerId") Long customerId,
                                             @Param("fromDate") java.time.LocalDateTime fromDate,
                                             @Param("toDate") java.time.LocalDateTime toDate,
                                             Pageable pageable);

    @Query("select i from Interaction i where i.delFlag = false and i.status = 'FOLLOW_UP' and i.remindAt <= :until order by i.remindAt asc")
    Page<Interaction> findDueReminders(@Param("until") java.time.LocalDateTime until, Pageable pageable);

    @Modifying
    @Query("update Interaction i set i.customerId = :targetId where i.customerId = :sourceId and i.delFlag = false")
    int updateCustomerId(@Param("sourceId") Long sourceId, @Param("targetId") Long targetId);
}
