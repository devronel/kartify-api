package com.kartify.api.payment.dto;

import java.time.Instant;

public record LinkPaidEvent(
  String linkdId,
  int amountCentavos,
  Instant paidAt
) {}
