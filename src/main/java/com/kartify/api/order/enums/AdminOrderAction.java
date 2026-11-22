package com.kartify.api.order.enums;

public enum AdminOrderAction {
  START_PROCESSING(OrderStatus.PROCESSING),
  MARK_SHIPPED(OrderStatus.SHIPPED),
  MARK_DELIVERED(OrderStatus.DELIVERED),
  CANCEL(OrderStatus.CANCELLED);

  private final OrderStatus target;
  
  AdminOrderAction(OrderStatus target){
    this.target = target;
  }

  public OrderStatus target(){ return target; }
}
