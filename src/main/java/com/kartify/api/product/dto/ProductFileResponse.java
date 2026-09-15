package com.kartify.api.product.dto;

import com.kartify.api.shared.dto.UploadedFileResponse;

public record ProductFileResponse(
    UploadedFileResponse file,
    Boolean isPrimary
) {}
