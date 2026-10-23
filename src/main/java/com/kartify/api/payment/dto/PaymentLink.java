package com.kartify.api.payment.dto;

public record PaymentLink(
  String id,
  String checkoutUrl,
  String payload
){}
