package com.kartify.api.product.dto;

import org.springframework.web.multipart.MultipartFile;

public record ProductFileRequest(
    MultipartFile file,
    Boolean isPrimary
) {}
