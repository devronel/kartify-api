package com.kartify.api.order.dto;

import java.math.BigDecimal;

public record OrderItemsResponse(
  String productName,
  String variantLabel,
  String productImage,
  BigDecimal price,
  Integer quantity,
  BigDecimal lineTotal
) {}
