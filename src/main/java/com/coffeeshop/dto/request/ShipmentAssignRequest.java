package com.coffeeshop.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShipmentAssignRequest {

    @NotNull
    private Long shipperId;

    private String address;
}
