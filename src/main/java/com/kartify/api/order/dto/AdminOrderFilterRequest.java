package com.kartify.api.order.dto;

import java.time.LocalDate;

public record AdminOrderFilterRequest(
  String search,
  String status,
  LocalDate dateFrom,
  LocalDate dateTo
) {}
