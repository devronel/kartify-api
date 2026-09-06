package com.kartify.api.product.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kartify.api.product.dto.ProductAttributeRequest;
import com.kartify.api.product.dto.ProductAttributeResponse;
import com.kartify.api.product.dto.ProductAttributeValueRequest;
import com.kartify.api.product.dto.ProductAttributeValueResponse;
import com.kartify.api.product.service.ProductAttributeService;
import com.kartify.api.shared.dto.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/product/attribute")
public class ProductAttributeController {

    private final ProductAttributeService productAttributeService;

    public ProductAttributeController(ProductAttributeService productAttributeService){
        this.productAttributeService = productAttributeService;
    }

    // --- Create product variant attribute ---
    @PostMapping
    public ResponseEntity<ApiResponse<ProductAttributeResponse>> create(
        @Valid @RequestBody ProductAttributeRequest request
    ){
        ProductAttributeResponse createProductAttribute = productAttributeService.create(request);
        return ResponseEntity.ok(ApiResponse.success("Product Variant Attribute Created", createProductAttribute));
    }

    // --- Create product variant attribute value ---
    @PostMapping("/values")
    public ResponseEntity<ApiResponse<ProductAttributeValueResponse>> createAttributeValue(
        @Valid @RequestBody ProductAttributeValueRequest request
    ){
        ProductAttributeValueResponse attributeValueResponse = productAttributeService.createAttributeValue(request);
        return ResponseEntity.ok(ApiResponse.success("Product Variant Attribute Value Created", attributeValueResponse));
    }

}
