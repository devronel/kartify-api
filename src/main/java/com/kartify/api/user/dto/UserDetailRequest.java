package com.kartify.api.user.dto;

import java.time.LocalDate;

import com.kartify.api.user.enums.Gender;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserDetailRequest(
    @NotBlank(message = "First name is required") String firstName,
    @NotBlank(message = "Last name is required") String lastName,
    @NotBlank(message = "Phone number is required") @Size(max = 30, message = "Phone must be at most 30 characters") String phone,
    LocalDate dateOfBirth,
    @NotBlank(message = "Gender is required") Gender gender
) {}
