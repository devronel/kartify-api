package com.kartify.api.exception;

public class PaymentCreationUnknownException extends RuntimeException {
  
  public PaymentCreationUnknownException(String message) {
    super(message);
  }

  public PaymentCreationUnknownException(String message, Throwable cause) {
    super(message, cause);
  }
}
