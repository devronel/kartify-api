package com.kartify.api.product.dto;

public record ProductEditFileResponse(
    Long id,
    String fileUrl,
    Boolean isPrimary
) {}
