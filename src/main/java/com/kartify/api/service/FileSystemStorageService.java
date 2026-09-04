package com.kartify.api.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.kartify.api.contract.FileStorage;
import com.kartify.api.shared.dto.UploadedFileResponse;

@Primary
@Service
public class FileSystemStorageService implements FileStorage {

    private final String baseUrl;
    private final Path root;

    public FileSystemStorageService(
        @Value("${app.storage.location}") String storageLocation,
        @Value("${app.base-url}") String baseUrl
    ){
        this.root = Paths.get(storageLocation).toAbsolutePath().normalize();
        this.baseUrl = baseUrl;
        try { Files.createDirectories(root); } catch (IOException e) { throw new RuntimeException(e); }
    }

    @Override
    public UploadedFileResponse upload(MultipartFile file, String folder){
        try {

            Path filePath = this.root;

            if (folder != null && !folder.isBlank()) {
                filePath = filePath.resolve(folder);
            }

            Files.createDirectories(filePath);

            String originalName = file.getOriginalFilename();
            Long size = file.getSize();
            String mimeType = file.getContentType();
            String extension = "";
            if (originalName.lastIndexOf(".") >= 0) {
                extension = originalName.substring(originalName.lastIndexOf(".") + 1);
            }
            String fileName = UUID.randomUUID() + "." + extension;

            Path target = filePath.resolve(fileName).normalize();

            InputStream fileStream = file.getInputStream();

            Files.copy(fileStream, target, StandardCopyOption.REPLACE_EXISTING);

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

    @Override
    public Resource loadAsResource(String filename) {
        try {
            Path file = root.resolve(filename).normalize();
    
            if (!file.startsWith(root.normalize())) {
                throw new SecurityException("Invalid path");
            }
    
            if (!Files.exists(file) || !Files.isRegularFile(file)) {
                throw new NoSuchFileException(filename);
            }
    
            Resource resource = new UrlResource(file.toUri());
    
            if (!resource.exists() || !resource.isReadable()) {
                throw new IOException("File is not readable: " + filename);
            }
    
            return resource;
        } catch (IOException ex) {
            throw new RuntimeException("Failed to fetch file", ex);
        }
    }

    @Override
    public String getUrl(String path) {
        return baseUrl + "/api/" + path;
    }
}
