package com.kartify.api.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoryUpdateRequest(
    Long parentId,
    
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    String name,

    @Size(max = 255, message = "Description must not exceed 255 characters")
    String description,

    @NotNull(message = "Status is required.")
    Boolean isActive
) {}
