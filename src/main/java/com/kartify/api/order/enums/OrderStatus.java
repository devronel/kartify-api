package com.kartify.api.order.enums;

import java.util.List;

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

  public List<AdminOrderAction> adminActions(){
    return switch (this) {
      case PENDING -> List.of(AdminOrderAction.CANCEL);
      case PAID -> List.of(AdminOrderAction.START_PROCESSING);
      case PROCESSING -> List.of(AdminOrderAction.MARK_SHIPPED);
      case SHIPPED -> List.of(AdminOrderAction.MARK_DELIVERED);
      case DELIVERED, CANCELLED -> List.of();
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
