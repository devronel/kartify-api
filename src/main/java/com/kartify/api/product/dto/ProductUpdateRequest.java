package com.kartify.api.product.dto;

import java.math.BigDecimal;
import java.util.List;

import com.kartify.api.contract.ProductVariantPayload;
import com.kartify.api.product.validator.annotation.ValidProductVariant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@ValidProductVariant
public record ProductUpdateRequest(

    @NotNull(message = "Category is required")
    Long categoryId,

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    String name,

    @NotBlank(message = "Slug is required")
    String slug,

    String description,

    @Size(max = 255, message = "Short description must not exceed 255 characters")
    String shortDescription,

    @Size(max = 100, message = "Sku must not exceed 100 characters")
    String sku,

    @NotNull(message = "Price is required")
    BigDecimal price,

    BigDecimal comparePrice,

    BigDecimal costPrice,

    Boolean hasVariant,

    Integer stockQuantity,

    BigDecimal weight,

    List<ProductUpdateFileRequest> files,

    List<@Valid ProductUpdateVariantRequest> variants
) implements ProductVariantPayload {}
