package com.kartify.api.category.dto;

import java.util.List;

public record CategoryTreeResponse(
    Long id,
    String name,
    String slug,
    Boolean isActive,
    Integer productCount,
    List<CategoryTreeResponse>children
) {}
