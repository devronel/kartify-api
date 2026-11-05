package com.kartify.api.order.dto;

import jakarta.validation.constraints.NotNull;

public record OrderCreateRequest(
  
  @NotNull(message = "Address is required.")
  Long addressId

) {}
