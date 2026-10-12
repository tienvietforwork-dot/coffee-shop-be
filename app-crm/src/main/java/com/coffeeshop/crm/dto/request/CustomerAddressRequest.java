package com.coffeeshop.crm.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CustomerAddressRequest {
    @Size(max = 30)
    private String label;
    @NotBlank
    @Size(max = 100)
    private String recipientName;
    @NotBlank
    @Size(max = 15)
    private String recipientPhone;
    @NotBlank
    @Size(max = 255)
    private String address;
    private Boolean isDefault;
}
