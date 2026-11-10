package com.kartify.api.order.enums;

public enum PaymentStatus {
  CREATING,
  PENDING,
  SUCCEEDED,
  FAILED,
  SETUP_FAILED, // Payment Provider failed to create the Payment Link.
  CREATION_UNKNOWN, // System doesn't know whether Payment Provider created the link, such as after a timeout.
  EXPIRED,
  REFUNDED;

  public PaymentAction paymentAction() {
    return switch (this) {
      case PENDING -> PaymentAction.PAY;
      case CREATING, SETUP_FAILED, CREATION_UNKNOWN, FAILED, EXPIRED -> PaymentAction.RETRY;
      case SUCCEEDED, REFUNDED -> PaymentAction.NONE;
    };
  }
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
