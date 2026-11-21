package com.kartify.api.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.kartify.api.order.enums.OrderStatus;
import com.kartify.api.order.enums.PaymentStatus;

public record AdminOrderResponse(
  String orderNumber,
  String recipientName,
  OrderStatus orderStatus,
  PaymentStatus paymentStatus,
  BigDecimal total,
  Integer orderItemCount,
  String cityAndProvince,
  LocalDateTime createdAt,
  LocalDateTime statusUpdatedAt,
  boolean isExpired
) {}
