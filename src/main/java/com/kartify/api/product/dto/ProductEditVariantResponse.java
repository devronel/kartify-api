package com.kartify.api.product.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProductEditVariantResponse(
    Long id,
    List<Long> attributeIds,
    String sku,
    BigDecimal price,
    BigDecimal comparePrice,
    BigDecimal costPrice,
    Integer stockQuantity,
    BigDecimal weight,
    Boolean isActive
) {}
