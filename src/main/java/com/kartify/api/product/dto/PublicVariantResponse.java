package com.kartify.api.product.dto;

import java.math.BigDecimal;
import java.util.List;

public record PublicVariantResponse(
  Long id,
  List<Long> attributeValueIds,
  String sku,
  BigDecimal price,
  Boolean inStock
) {}
