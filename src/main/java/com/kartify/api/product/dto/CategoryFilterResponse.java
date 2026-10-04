package com.kartify.api.product.dto;

import java.util.List;

public record CategoryFilterResponse(
  Long id,
  String name,
  String slug,
  Integer productCount,
  List<CategoryFilterResponse> child
) {}