package com.kartify.api.order.dto;

import com.kartify.api.payment.enums.PaymentMethod;

import jakarta.validation.constraints.NotNull;

public record OrderCreateRequest(
  
  @NotNull(message = "Address is required.")
  Long addressId,

  @NotNull(message = "Payment method is required.")
  PaymentMethod paymentMethod

) {}
