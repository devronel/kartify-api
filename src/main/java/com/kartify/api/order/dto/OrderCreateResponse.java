package com.kartify.api.order.dto;

import java.math.BigDecimal;

public record OrderCreateResponse(
  Long orderId,
  BigDecimal totalAmount
){}
