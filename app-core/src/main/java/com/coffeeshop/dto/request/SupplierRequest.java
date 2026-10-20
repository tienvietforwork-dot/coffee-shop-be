package com.coffeeshop.dto.request;

import com.coffeeshop.entity.enums.SupplierStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/** {@code materials}: what the supplier sells, replaced as a whole on every save. */
public record SupplierRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 100) String contactName,
        @Size(max = 15) String phone,
        @Email @Size(max = 100) String email,
        @Size(max = 255) String address,
        @Size(max = 20) String taxCode,
        @Size(max = 255) String note,
        SupplierStatus status,
        List<@Valid Item> materials) {

    /** {@code unitPrice}: reference price per unit of the material */
    public record Item(@NotNull Long materialId, Long packageId, @DecimalMin("0") BigDecimal unitPrice) {
    }
}
