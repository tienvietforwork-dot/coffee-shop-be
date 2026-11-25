package com.coffeeshop.promotions.dto.response;

import com.coffeeshop.promotions.entity.Customer;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomerResponse {

    private Long id;
    private String fullName;
    private String phone;
    private String email;
    private LocalDate dateOfBirth;
    private String gender;
    private String description;
    private Integer loyaltyPoints;
    private LocalDateTime registeredAt;

    public CustomerResponse(Customer customer) {
        Objects.requireNonNull(customer, "customer must not be null");
        this.id = customer.getId();
        this.fullName = customer.getFullName();
        this.phone = customer.getPhone();
        this.email = customer.getEmail();
        this.dateOfBirth = customer.getDateOfBirth();
        this.gender = customer.getGender();
        this.description = customer.getDescription();
        this.loyaltyPoints = customer.getLoyaltyPoints();
        this.registeredAt = customer.getRegisteredAt();
    }
}