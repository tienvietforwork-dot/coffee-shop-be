package com.coffeeshop.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record ProfileRequest(@Size(max = 100) String fullName, @Email @Size(max = 100) String email) {
}
