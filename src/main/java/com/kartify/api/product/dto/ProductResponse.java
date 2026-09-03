package com.kartify.api.product.dto;

import java.math.BigDecimal;

public record ProductResponse(
    String name,
    String slug,
    String description,
    String shortDescription,
    String sku,
    BigDecimal price,
    BigDecimal comparePrice,
    BigDecimal costPrice,
    Boolean hasVariants,
    Integer stockQuantity,
    BigDecimal weight,
    Boolean isActive,
    Boolean isFeatured
){}
