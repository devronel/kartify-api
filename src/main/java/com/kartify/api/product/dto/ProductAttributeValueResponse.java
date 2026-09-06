package com.kartify.api.product.dto;

public record ProductAttributeValueResponse(
    Long id,
    Long productAttributeId,
    String productAttributeValueName
) {}
