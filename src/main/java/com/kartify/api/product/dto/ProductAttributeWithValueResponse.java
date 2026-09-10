package com.kartify.api.product.dto;

import java.util.List;

public record ProductAttributeWithValueResponse(
    Long id,
    String name,
    List<ProductAttributeValueResponse> values
) {}
