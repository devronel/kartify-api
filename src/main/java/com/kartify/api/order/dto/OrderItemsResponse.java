package com.kartify.api.order.dto;

import java.math.BigDecimal;

public record OrderItemsResponse(
  Long id,
  String productName,
  String variantLabel,
  String productImage,
  BigDecimal price,
  Integer quantity,
  BigDecimal lineTotal
) {}
