package com.kartify.api.contract;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorage {
    String upload(MultipartFile file);
}
