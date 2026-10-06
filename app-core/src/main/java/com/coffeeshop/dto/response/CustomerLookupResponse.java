package com.coffeeshop.dto.response;

import com.coffeeshop.entity.Customer;
import com.coffeeshop.entity.CustomerAddress;

import java.util.List;

public record CustomerLookupResponse(Long id, String fullName, String phone, Integer loyaltyPoints,
                                     List<Address> addresses) {

    public record Address(Long id, String label, String recipientName, String recipientPhone, String address,
                          boolean isDefault) {
    }

    public static CustomerLookupResponse from(Customer c, List<CustomerAddress> addresses) {
        return new CustomerLookupResponse(c.getId(), c.getFullName(), c.getPhone(), c.getLoyaltyPoints(),
                addresses.stream().map(a -> new Address(a.getId(), a.getLabel(), a.getRecipientName(),
                        a.getRecipientPhone(), a.getAddress(), a.isDefaultAddress())).toList());
    }
}
