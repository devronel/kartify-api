package com.kartify.api.order.dto;

import java.math.BigDecimal;
import java.util.List;

public record CheckoutDetailsResponse(
  List<AddressInformationResponse> addresses,
  List<CheckoutCartItemResponse> cartItems,
  BigDecimal subtotal,
  BigDecimal total
) {}
