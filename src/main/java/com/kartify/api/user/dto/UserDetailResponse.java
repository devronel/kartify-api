package com.kartify.api.user.dto;

import java.time.LocalDate;

public record UserDetailResponse(
    Long userId,
    String firstName,
    String lastName,
    String phone,
    LocalDate dateOfBirth,
    String gender
) {}
