package com.kartify.api.product.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ProductVariantRequest(
    @NotEmpty(message = "At least one attribute value is required")
    List<@NotNull @Positive Long> attributeValueIds,

    @NotBlank(message = "SKU is required")
    String sku,

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", message = "Price cannot be negative")
    BigDecimal price,

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity cannot be negative")
    Integer stockQuantity,

    @NotNull(message = "Status is required")
    Boolean isActive
) {}
