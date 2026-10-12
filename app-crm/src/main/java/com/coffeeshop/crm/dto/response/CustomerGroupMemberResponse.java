package com.coffeeshop.crm.dto.response;

import com.coffeeshop.crm.entity.CustomerGroupMember;
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
        Boolean isManual,
        Boolean isExcluded,
        LocalDateTime addedAt
) {
    public static CustomerGroupMemberResponse from(CustomerGroupMember member) {
        return new CustomerGroupMemberResponse(
                member.getId(), member.getCreatedBy(), member.getCreatedAt(),
                member.getUpdatedBy(), member.getUpdatedAt(), member.getDelFlag(), member.getDelUser(),
                member.getGroupId(), member.getCustomerId(), member.getIsManual(), member.getIsExcluded(),
                member.getAddedAt());
    }
}
