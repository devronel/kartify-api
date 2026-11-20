package com.kartify.api.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

import com.kartify.api.order.enums.PaymentMethod;
import com.kartify.api.order.enums.PaymentStatus;

public record AdminOrderPaymentResponse(
  PaymentStatus status,
  PaymentMethod paymentMethod,
  String provider,
  String providerPaymentId,
  BigDecimal amount,
  Instant paidAt,
  LocalDateTime createdAt
) {}
