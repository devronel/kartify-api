package com.kartify.api.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.kartify.api.order.enums.OrderStatus;
import com.kartify.api.order.enums.PaymentAction;
import com.kartify.api.order.enums.PaymentStatus;

public record OrderResponse(
  String orderNumber,
  OrderStatus status,
  PaymentStatus paymentStatus,
  String checkoutUrl,
  BigDecimal total,
  Integer itemCount,
  PaymentAction paymentAction,
  boolean isExpired,
  LocalDateTime expiresAt,
  LocalDateTime createdAt,
  List<OrderItemsResponse> orderItems
) {}
