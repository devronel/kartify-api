package com.kartify.api.auth.dto;

import com.kartify.api.user.enums.Role;

public record AuthResponse (
    Long id,
    String email,
    String firstName,
    String lastName,
    Role role
) {}
