package com.kartify.api.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.kartify.api.contract.FileStorage;
import com.kartify.api.shared.dto.UploadedFileResponse;

import io.imagekit.client.ImageKitClient;
import io.imagekit.client.okhttp.ImageKitOkHttpClient;
import io.imagekit.models.files.FileUploadParams;
import io.imagekit.models.files.FileUploadResponse;

@Service
public class ImageKitService implements FileStorage {

    private final ImageKitClient client;

    public ImageKitService(@Value("${imagekit.private-key}") String privateKey){
        this.client = ImageKitOkHttpClient.builder()
            .privateKey(privateKey)
            .build();
    }

    @Override
    public UploadedFileResponse upload(MultipartFile file){
        try {
            String originalName = file.getOriginalFilename();
            Long size = file.getSize();
            String mimeType = file.getContentType();
            String extension = "";
            if (originalName.lastIndexOf(".") >= 0) {
                extension = originalName.substring(originalName.lastIndexOf(".") + 1);
            }
            String fileName = UUID.randomUUID() + "." + extension;

            InputStream stream = file.getInputStream();
            FileUploadParams params = FileUploadParams.builder()
                .file(stream)
                .folder("/kartify/profile")
                .fileName(fileName)
                .useUniqueFileName(false)
                .build();

            client.files().upload(params);

            return new UploadedFileResponse(
                originalName,
                fileName,
                size,
                extension,
                mimeType
            );
        } catch(IOException ex){
            throw new RuntimeException("Failed to upload file", ex);
        }
    }
}
