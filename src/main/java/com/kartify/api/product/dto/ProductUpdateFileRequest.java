package com.kartify.api.product.dto;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotNull;

public record ProductUpdateFileRequest(
    Long id,

    String fileUrl,

    MultipartFile file,

    @NotNull(message = "Is Primary is required")
    Boolean isPrimary
) {}
