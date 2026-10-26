package com.kartify.api.order.enums;

public enum OrderStatus {
  PENDING,
  PAID,
  PROCESSING,
  SHIPPED,
  DELIVERED,
  CANCELLED;

  public boolean canTransitionTo(OrderStatus next){
    return switch(this) {
      case PENDING  -> next == PAID || next == CANCELLED;
      case PAID     -> next == PROCESSING || next == CANCELLED;
      case PROCESSING -> next == SHIPPED || next == CANCELLED;
      case SHIPPED    -> next == DELIVERED;
      case DELIVERED, CANCELLED -> false; 
    };
  }

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
