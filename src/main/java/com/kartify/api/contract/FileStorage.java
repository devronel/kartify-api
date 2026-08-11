package com.kartify.api.contract;

import org.springframework.web.multipart.MultipartFile;

import com.kartify.api.shared.dto.UploadedFileResponse;

import io.imagekit.models.files.FileUploadResponse;

public interface FileStorage {
    UploadedFileResponse upload(MultipartFile file);
}
