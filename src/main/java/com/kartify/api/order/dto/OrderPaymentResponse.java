package com.kartify.api.order.dto;

import java.math.BigDecimal;

public record OrderPaymentResponse(
  Long orderId,
  Long paymentId,
  String orderNumber,
  BigDecimal totalAmount
) {}
