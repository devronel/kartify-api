package com.kartify.api.contract;

import org.springframework.web.multipart.MultipartFile;

import com.kartify.api.shared.dto.UploadedFileResponse;

public interface FileStorage {
    UploadedFileResponse upload(MultipartFile file, String folder);
}
