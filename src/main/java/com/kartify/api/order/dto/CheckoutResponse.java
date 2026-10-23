package com.kartify.api.order.dto;

public record CheckoutResponse(
  String orderNumber,
  String checkoutUrl
) {}
