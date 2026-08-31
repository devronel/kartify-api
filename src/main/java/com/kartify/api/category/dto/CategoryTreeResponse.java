package com.kartify.api.category.dto;

import java.util.List;

public record CategoryTreeResponse(
    Long id,
    Long parentId,
    String name,
    String slug,
    Boolean isActive,
    Integer productCount,
    List<CategoryTreeResponse>children
) {}
