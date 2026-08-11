package com.kartify.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.kartify.api.contract.FileStorage;

import io.imagekit.client.ImageKitClient;
import io.imagekit.client.okhttp.ImageKitOkHttpClient;

@Service
public class ImageKitService implements FileStorage {

    private final ImageKitClient client;

    public ImageKitService(@Value("${imagekit.private-key}") String privateKey){
        this.client = ImageKitOkHttpClient.builder()
            .privateKey(privateKey)
            .build();
    }

    @Override
    public String upload(MultipartFile file){
        return "Test";
    }
}
