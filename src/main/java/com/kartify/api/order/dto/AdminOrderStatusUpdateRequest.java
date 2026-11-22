package com.kartify.api.order.dto;

import com.kartify.api.order.enums.OrderStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminOrderStatusUpdateRequest(
  @NotNull OrderStatus status,
  @Size(max = 255) String notes
) {}
