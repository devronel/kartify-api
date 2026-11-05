package com.kartify.api.order.dto;

import java.math.BigDecimal;

public record CheckoutCartItemResponse(
  Long id,
  String productImage,
  String productName,
  String variantLabel,
  Integer quantity,
  BigDecimal price
) {}
