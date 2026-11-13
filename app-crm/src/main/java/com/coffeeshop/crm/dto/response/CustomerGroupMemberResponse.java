package com.coffeeshop.crm.dto.response;

import com.coffeeshop.crm.entity.CustomerGroupMember;
import com.coffeeshop.crm.entity.Customer;
import java.time.LocalDateTime;

public record CustomerGroupMemberResponse(
        Long id,
        String createdBy,
        LocalDateTime createdAt,
        String updatedBy,
        LocalDateTime updatedAt,
        Boolean delFlag,
        String delUser,
        Long groupId,
        Long customerId,
        String customerName,
        String customerPhone,
        Boolean isManual,
        Boolean isExcluded,
        LocalDateTime addedAt
) {
    public static CustomerGroupMemberResponse from(CustomerGroupMember member) {
        return new CustomerGroupMemberResponse(
                member.getId(), member.getCreatedBy(), member.getCreatedAt(),
                member.getUpdatedBy(), member.getUpdatedAt(), member.getDelFlag(), member.getDelUser(),
                member.getGroupId(), member.getCustomerId(), null, null, member.getIsManual(), member.getIsExcluded(),
                member.getAddedAt());
    }

    public static CustomerGroupMemberResponse from(CustomerGroupMember member, Customer customer) {
        return new CustomerGroupMemberResponse(
                member.getId(), member.getCreatedBy(), member.getCreatedAt(),
                member.getUpdatedBy(), member.getUpdatedAt(), member.getDelFlag(), member.getDelUser(),
                member.getGroupId(), member.getCustomerId(),
                customer == null ? null : customer.getFullName(), customer == null ? null : customer.getPhone(),
                member.getIsManual(), member.getIsExcluded(), member.getAddedAt());
    }
}
