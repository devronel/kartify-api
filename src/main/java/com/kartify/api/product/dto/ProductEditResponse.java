package com.kartify.api.product.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProductEditResponse(
    ProductCategoryResponse category,
    String name,
    String slug,
    String description,
    String shortDescription,
    String sku,
    BigDecimal price,
    BigDecimal comparePrice,
    BigDecimal costPrice,
    Boolean hasVariant,
    Integer stockQuantity,
    BigDecimal weight,
    Boolean isActive,
    Boolean isFeatured,
    List<ProductEditFileResponse> files,
    List<ProductEditVariantResponse> variants
) {}
