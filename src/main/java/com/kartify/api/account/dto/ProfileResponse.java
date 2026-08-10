package com.kartify.api.account.dto;

import java.time.LocalDate;

import com.kartify.api.user.enums.Gender;

public record ProfileResponse(
    String firstName,
    String lastName,
    String phone,
    LocalDate dateOfBirth,
    Gender gender
) {}
