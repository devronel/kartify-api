package com.kartify.api.cart.dto;

import java.math.BigDecimal;

public record ItemQuantityUpdateResponse(
  Long id,
  Integer quantity,
  BigDecimal subtotal
) {}
