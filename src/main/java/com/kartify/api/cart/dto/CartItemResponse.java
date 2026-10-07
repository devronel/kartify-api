package com.kartify.api.cart.dto;

import java.math.BigDecimal;

public record CartItemResponse(
  Long id,
  Long productId,
  Long productVariantId,
  String name,
  String imageUrl,
  Integer quantity,
  BigDecimal price,
  BigDecimal subtotal
) {}
