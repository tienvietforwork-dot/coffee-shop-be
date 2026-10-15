package com.coffeeshop.crm.dto.response;

import com.coffeeshop.crm.entity.CustomerGroup;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;

public record CustomerGroupResponse(
        Long id,
        String createdBy,
        LocalDateTime createdAt,
        String updatedBy,
        LocalDateTime updatedAt,
        Boolean delFlag,
        String delUser,
        String name,
        String description,
        String groupType,
        JsonNode criteriaRule,
        LocalDateTime recalculatedAt
) {
    public static CustomerGroupResponse from(CustomerGroup group) {
        return new CustomerGroupResponse(
                group.getId(), group.getCreatedBy(), group.getCreatedAt(),
                group.getUpdatedBy(), group.getUpdatedAt(), group.getDelFlag(), group.getDelUser(),
                group.getName(), group.getDescription(), group.getGroupType(), group.getCriteriaRule(),
                group.getRecalculatedAt());
    }
}
