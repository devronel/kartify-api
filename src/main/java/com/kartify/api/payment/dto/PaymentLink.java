package com.kartify.api.payment.dto;

public record PaymentLink(
  String paymentLinkId,
  String checkoutUrl,
  String payload
){}
