package com.kartify.api.product.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductAttributeValueRequest(
    @NotNull(message = "Product attribute is Request")
    Long productAttributeId,

    @Size(max = 100, message = "Value must not exceed 100 characters")
    String productAttributeValueName
) {}
