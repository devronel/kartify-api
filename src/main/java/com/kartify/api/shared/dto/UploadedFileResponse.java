package com.kartify.api.shared.dto;

public record UploadedFileResponse(
    String originalName,
    String fileName,
    Long size,
    String extension,
    String mimeType
){}
