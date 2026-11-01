package com.kartify.api.order.dto;

import java.math.BigDecimal;

public record PaymentAttemptResponse(
  Long orderId,
  Long paymentId,
  String idempotencyKey,
  BigDecimal totalAmount,
  String orderNumber
) {}
