package com.kartify.api.cart.dto;

import com.kartify.api.cart.enums.ItemProblemReason;

public record ItemProblem(
  Long cartItemId,
  String name,
  ItemProblemReason reason,
  Integer availableStock
){}
