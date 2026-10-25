package com.kartify.api.payment.dto;

public record PaymentLink(
  String paymentId,
  String checkoutUrl,
  String payload
){}
