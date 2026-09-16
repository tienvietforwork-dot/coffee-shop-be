package com.coffeeshop.dto.request;

import com.coffeeshop.entity.enums.OrderType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrderRequest {

    private String customerName;

    private String customerPhone;

    @NotNull
    private OrderType orderType;

    @NotEmpty
    @Valid
    private List<OrderItemRequest> items;
}
