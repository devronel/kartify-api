package com.kartify.api.exception;

public class PaymentCreationRejectedException extends RuntimeException {
  public PaymentCreationRejectedException(String message) {
    super(message);
  }
}
