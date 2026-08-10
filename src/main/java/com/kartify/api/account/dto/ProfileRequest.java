package com.kartify.api.account.dto;

import java.time.LocalDate;

import com.kartify.api.user.enums.Gender;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProfileRequest(
    @NotBlank(message = "First name is required.") @Size(max = 100) String firstName,
    @NotBlank(message = "Last name is required.") @Size(max = 100) String lastName,

    @NotBlank(message = "Phone number is required.") 
    @Size(max = 30) 
    @Pattern(
        regexp = "^(09|\\+639)\\d{9}$", 
        message = "Invalid phone number. Use format 09XXXXXXXXX or +639XXXXXXXXX"
    )
    String phone,

    LocalDate dateOfBirth,
    @NotNull Gender gender
){}
