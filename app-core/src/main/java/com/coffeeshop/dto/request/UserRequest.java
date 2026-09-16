package com.coffeeshop.dto.request;

import com.coffeeshop.entity.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRequest {

    @NotBlank
    private String username;

    // Optional on update (leave blank to keep current password)
    private String password;

    private String fullName;

    @Email
    private String email;

    private String phone;

    @NotNull
    private Role role;

    private Boolean active;
}
