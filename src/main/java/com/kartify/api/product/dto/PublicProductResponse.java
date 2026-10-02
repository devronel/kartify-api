package com.kartify.api.product.dto;

import java.math.BigDecimal;

public record PublicProductResponse(
  Long id,
  String category,
  String name,
  String slug,
  String description,
  String shortDescription,
  BigDecimal price,
  BigDecimal comparePrice,
  Boolean hasVariant,
  String primaryImage
) {}
