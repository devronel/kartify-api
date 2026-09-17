package com.kartify.api.product.dto;

import java.math.BigDecimal;

public record ProductAdminListResponse(
    Long id,
    String category,
    String name,
    String sku,
    BigDecimal price,
    BigDecimal comparePrice,
    BigDecimal costPrice,
    Boolean hasVariants,
    Integer stockQuantity,
    BigDecimal weight,
    Boolean isActive,
    Boolean isFeatured,
    String primaryImage
) {}
