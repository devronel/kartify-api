package com.kartify.api.order.dto;

import java.math.BigDecimal;

public record AdminOrderItemResponse(
  Long id,
  String productName,
  String variantLabel,
  String sku,
  String productImage,
  BigDecimal price,
  Integer quantity,
  BigDecimal lineTotal
) {}
