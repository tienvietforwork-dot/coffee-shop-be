package com.coffeeshop.promotions.dto.response;

import com.coffeeshop.promotions.entity.CustomerGroup;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomerGroupResponse {

    private Long id;
    private String name;
    private String description;
    private String groupType;
    private String classificationCode;
    private JsonNode criteriaRule;
    private LocalDateTime recalculatedAt;

    public CustomerGroupResponse(CustomerGroup group) {
        Objects.requireNonNull(group, "customerGroup must not be null");
        this.id = group.getId();
        this.name = group.getName();
        this.description = group.getDescription();
        this.groupType = group.getGroupType();
        this.classificationCode = group.getClassificationCode();
        this.criteriaRule = group.getCriteriaRule();
        this.recalculatedAt = group.getRecalculatedAt();
    }
}