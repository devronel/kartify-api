package com.kartify.api.product.dto;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductCreateRequest(

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

    @NotBlank(message = "Sku is required")
    @Size(max = 100, message = "Sku must not exceed 100 characters")
    String sku,

    @NotNull(message = "Price is required")
    BigDecimal price,

    BigDecimal comparePrice,

    BigDecimal costPrice,

    Boolean hasVariants,

    Integer stockQuantity,

    BigDecimal weight,

    Boolean isActive,

    Boolean isFeatured,

    List<MultipartFile> images
){}
