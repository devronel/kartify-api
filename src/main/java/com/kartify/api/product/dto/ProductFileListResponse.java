package com.kartify.api.product.dto;

public record ProductFileListResponse(
    Long id,
    Long productId,
    String filename,
    String name,
    Long size,
    String extension,
    String mimeType,
    Boolean isPrimary
) {}
