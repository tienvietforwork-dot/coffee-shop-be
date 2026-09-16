package com.coffeeshop.dto.request;

import com.coffeeshop.entity.enums.ShipmentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShipmentStatusUpdateRequest {

    @NotNull
    private ShipmentStatus status;

    private String note;
}
