package com.kartify.api.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.kartify.api.order.enums.OrderStatus;
import com.kartify.api.order.enums.PaymentStatus;

public record OrderResponse(
  String orderNumber,
  OrderStatus status,
  PaymentStatus paymentStatus,
  BigDecimal total,
  Integer itemCount,
  Boolean canPay,
  LocalDateTime expireAt
) {}
