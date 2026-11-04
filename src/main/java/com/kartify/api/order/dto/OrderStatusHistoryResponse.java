package com.kartify.api.order.dto;

import java.time.LocalDateTime;

import com.kartify.api.order.enums.OrderStatus;

public record OrderStatusHistoryResponse(
  OrderStatus status,
  LocalDateTime createdAt
) {}
