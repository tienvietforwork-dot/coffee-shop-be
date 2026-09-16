package com.coffeeshop.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShipmentCreateRequest {

    @NotNull
    private Long orderId;

    private String address;
}
