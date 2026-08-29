package com.kartify.api.category.dto;

public record CategoryResponse(
    Long id,
    Long parentId,
    String name,
    String slug,
    String description,
    Boolean isActive,
    Integer sortOrder
) {}
