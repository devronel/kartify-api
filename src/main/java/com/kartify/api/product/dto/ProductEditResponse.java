package com.kartify.api.product.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProductEditResponse(
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
    Boolean isFeatured,
    List<ProductEditFileResponse> files,
    List<ProductEditVariantResponse> variants
) {}
