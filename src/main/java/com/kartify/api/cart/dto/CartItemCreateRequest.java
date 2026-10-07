package com.kartify.api.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CartItemCreateRequest(
  @NotNull(message = "Product is required.")
  Long productId,

  Long productVariantId,

  @NotNull(message = "Quantity is required.")
  @Min(value = 1, message = "Quantity must be at least 1.")
  Integer quantity
) {}
