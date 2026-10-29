package com.kartify.api.order.dto;

import java.math.BigDecimal;

import com.kartify.api.order.enums.OrderStatus;

public record OrderResponse(
  String orderNumber,
  OrderStatus status,
  BigDecimal total,
  Integer itemCount
) {}
