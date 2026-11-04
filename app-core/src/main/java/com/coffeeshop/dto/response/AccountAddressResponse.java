package com.coffeeshop.dto.response;

import com.coffeeshop.entity.CustomerAddress;

public record AccountAddressResponse(Long id, String label, String recipientName, String recipientPhone,
                                     String address, boolean isDefault) {
    public static AccountAddressResponse from(CustomerAddress address) {
        return new AccountAddressResponse(address.getId(), address.getLabel(), address.getRecipientName(),
                address.getRecipientPhone(), address.getAddress(), address.isDefaultAddress());
    }
}
