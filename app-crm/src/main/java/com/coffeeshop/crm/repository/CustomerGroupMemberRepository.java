package com.coffeeshop.crm.repository;

import com.coffeeshop.crm.entity.CustomerGroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerGroupMemberRepository extends JpaRepository<CustomerGroupMember, Long> {
    interface CustomerGroupNameProjection {
        Long getCustomerId();
        String getGroupName();
    }

    List<CustomerGroupMember> findAllByGroupIdAndDelFlagFalse(Long groupId);
    List<CustomerGroupMember> findAllByGroupIdAndDelFlagFalseOrderByAddedAtDesc(Long groupId);
    List<CustomerGroupMember> findAllByGroupIdAndDelFlagFalseAndIsExcludedFalseOrderByAddedAtDesc(Long groupId);
    boolean existsByGroupIdAndCustomerIdAndDelFlagFalse(Long groupId, Long customerId);
    Optional<CustomerGroupMember> findByGroupIdAndCustomerIdAndDelFlagFalse(Long groupId, Long customerId);

    @Query("select m.customerId as customerId, g.name as groupName " +
            "from CustomerGroupMember m join CustomerGroup g on g.id = m.groupId " +
            "where m.customerId in :customerIds and m.isExcluded = false " +
            "order by m.customerId, g.name")
    List<CustomerGroupNameProjection> findGroupNamesByCustomerIds(@Param("customerIds") List<Long> customerIds);

    @Modifying
    @Query("update CustomerGroupMember m set m.delFlag = true, m.delUser = 'system', m.updatedAt = CURRENT_TIMESTAMP " +
            "where m.groupId = :groupId and m.isManual = false and m.delFlag = false")
    int softDeleteAutomaticMembers(@Param("groupId") Long groupId);

    @Modifying
    @Query("update CustomerGroupMember m set m.delFlag = true, m.delUser = 'system', m.updatedAt = CURRENT_TIMESTAMP " +
            "where m.groupId = :groupId and m.delFlag = false")
    int softDeleteGroupMembers(@Param("groupId") Long groupId);
}
