package com.kartify.api.category.dto;

public record CategoryResponse(
    Long id,
    CategoryResponse parent,
    String name,
    String slug,
    String description,
    Boolean isActive,
    Integer sortOrder
) {}
