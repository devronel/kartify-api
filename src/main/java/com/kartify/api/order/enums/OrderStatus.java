package com.kartify.api.order.enums;

public enum OrderStatus {
  PENDING,
  PAID,
  PROCESSING,
  SHIPPED,
  DELIVERED,
  CANCELLED
}

/*
  NOTE:

  Allowed Transaction

  PENDING     → PAID, CANCELLED
  PAID        → PROCESSING, CANCELLED
  PROCESSING  → SHIPPED, CANCELLED
  SHIPPED     → DELIVERED
  DELIVERED   → (final)
  CANCELLED   → (final)

  PAID and PROCESSING can be cancelled (with a refund), but once an order is SHIPPED, cancelling isn’t allowed, so it goes on to DELIVERED.
*/
