package com.kartify.api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetRequest(
    @NotBlank(message = "Email is required") @Email(message = "Invalid email format") @Size(max = 255) String email,
    @NotBlank(message = "Token is required") @Size(max = 255) String token,
    @NotBlank(message = "Password is required") @Size(min = 8, message = "Password must be at least 8 characters") String password,
    String confirmPassword
) {}
