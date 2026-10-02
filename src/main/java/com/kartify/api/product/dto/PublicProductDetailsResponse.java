package com.kartify.api.product.dto;

import java.math.BigDecimal;
import java.util.List;

public record PublicProductDetailsResponse(
  Long id,
  String name,
  String slug,
  String description,
  String shortDescription,
  BigDecimal price,
  BigDecimal comparePrice,
  Boolean hasVariant,
  Boolean inStock,
  Integer stockQuantity,
  List<String> images,
  List<ProductAttributeWithValueResponse> variantAttributes,
  List<PublicVariantResponse> variants
) {}
