package com.kartify.api.payment.dto;

import java.math.BigDecimal;

public record PaymentLinkRequest(
  BigDecimal amount,
  String description,
  String remarks
){}
