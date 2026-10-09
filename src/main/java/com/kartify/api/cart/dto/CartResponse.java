package com.kartify.api.cart.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
  List<CartItemResponse> items,
  Integer totalQuantity,
  BigDecimal subtotal
) {}
