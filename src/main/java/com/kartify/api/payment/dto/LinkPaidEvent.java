package com.kartify.api.payment.dto;

import java.time.Instant;

public record LinkPaidEvent(
  String paymentLinkId,
  int amountCentavos,
  Instant paidAt
) {}
