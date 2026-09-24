package com.kartify.api.product.dto;

public record ProductCategoryResponse(
    Long id,
    String name,
    Boolean isActive
) {}
