package com.kartify.api.product.dto;

public record FeaturedCategoryResponse(
  Long id,
  String name,
  long productCount
) {}
