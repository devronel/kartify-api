package com.kartify.api.product.controller;

import java.io.IOException;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kartify.api.product.service.ProductService;

@RestController
@RequestMapping("/api/files/public/product")
public class FileController {

    private final ProductService productService;

    public FileController(ProductService productService){
        this.productService = productService;
    }

    // --- Get Product image  by filename and return the actual image ---
    @GetMapping("/images/{filename}")
    public ResponseEntity<Resource> getImageByFilename(@PathVariable String filename) throws IOException{
        Resource img = productService.getImageByFilename(filename);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(img);
    }

}
