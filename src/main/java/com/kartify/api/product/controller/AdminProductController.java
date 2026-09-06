package com.kartify.api.product.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kartify.api.product.dto.ProductCreateRequest;
import com.kartify.api.product.dto.ProductResponse;
import com.kartify.api.product.service.ProductService;
import com.kartify.api.shared.dto.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/product")
public class AdminProductController {

    private final ProductService productService;

    public AdminProductController(ProductService productService){
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> create(@Valid @ModelAttribute ProductCreateRequest request){
        ProductResponse product = productService.create(request);
        return ResponseEntity.ok(ApiResponse.success("Product is Successfully Created.", product));
    }

}
