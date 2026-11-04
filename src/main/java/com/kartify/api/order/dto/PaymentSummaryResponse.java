package com.kartify.api.order.dto;

import java.time.Instant;

import com.kartify.api.order.enums.PaymentMethod;
import com.kartify.api.order.enums.PaymentStatus;

public record PaymentSummaryResponse(
  PaymentStatus status,
  PaymentMethod paymentMethod,
  String checkoutUrl,
  Instant paidAt
) {}
