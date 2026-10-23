package com.kartify.api.order.enums;

public enum PaymentStatus {
  PENDING,
  SUCCEEDED,
  FAILED,
  EXPIRED,
  REFUNDED
}

/*
  NOTE:

  Allowed Transaction

  PENDING    → SUCCEEDED, FAILED, EXPIRED
  SUCCEEDED  → REFUNDED
  FAILED     → (final)
  EXPIRED    → (final)
  REFUNDED   → (final)

*/
