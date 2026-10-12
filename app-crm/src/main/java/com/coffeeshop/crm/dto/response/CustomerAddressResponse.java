package com.coffeeshop.crm.dto.response;

import com.coffeeshop.crm.entity.CustomerAddress;
import java.time.LocalDateTime;

public record CustomerAddressResponse(
        Long id,
        String createdBy,
        LocalDateTime createdAt,
        String updatedBy,
        LocalDateTime updatedAt,
        Boolean delFlag,
        String delUser,
        Long customerId,
        String label,
        String recipientName,
        String recipientPhone,
        String address,
        Boolean isDefault
) {
    public static CustomerAddressResponse from(CustomerAddress address) {
        return new CustomerAddressResponse(
                address.getId(), address.getCreatedBy(), address.getCreatedAt(),
                address.getUpdatedBy(), address.getUpdatedAt(), address.getDelFlag(), address.getDelUser(),
                address.getCustomerId(), address.getLabel(), address.getRecipientName(), address.getRecipientPhone(),
                address.getAddress(), address.getIsDefault());
    }
}
