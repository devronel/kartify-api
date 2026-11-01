package com.kartify.api.payment.dto;

import java.math.BigDecimal;

public record PaymentLinkRequest(
  String idempotencyKey,
  BigDecimal amount,
  String description,
  String remarks
){}
