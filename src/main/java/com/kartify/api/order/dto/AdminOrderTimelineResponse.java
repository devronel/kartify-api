package com.kartify.api.order.dto;

import java.time.LocalDateTime;

import com.kartify.api.order.enums.OrderStatus;

public record AdminOrderTimelineResponse(
  OrderStatus status,
  String notes,
  String changeBy,
  LocalDateTime createdAt
) {}
