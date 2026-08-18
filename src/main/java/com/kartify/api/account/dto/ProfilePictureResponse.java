package com.kartify.api.account.dto;

import com.kartify.api.shared.dto.UploadedFileResponse;

public record ProfilePictureResponse(
    String url,
    UploadedFileResponse metadata
) {}
