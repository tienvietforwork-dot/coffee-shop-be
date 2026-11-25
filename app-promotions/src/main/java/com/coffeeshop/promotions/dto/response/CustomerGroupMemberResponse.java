package com.coffeeshop.promotions.dto.response;

import com.coffeeshop.promotions.entity.CustomerGroupMember;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomerGroupMemberResponse {

    private Long id;
    private Long groupId;
    private Long customerId;
    private Boolean isManual;
    private Boolean isExcluded;
    private LocalDateTime addedAt;

    public CustomerGroupMemberResponse(CustomerGroupMember member) {
        Objects.requireNonNull(member, "customerGroupMember must not be null");
        this.id = member.getId();
        this.groupId = member.getGroupId();
        this.customerId = member.getCustomerId();
        this.isManual = member.getIsManual();
        this.isExcluded = member.getIsExcluded();
        this.addedAt = member.getAddedAt();
    }
}