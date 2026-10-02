package com.kartify.api.product.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kartify.api.product.dto.PublicProductDetailsResponse;
import com.kartify.api.product.dto.PublicProductResponse;
import com.kartify.api.product.service.ProductService;
import com.kartify.api.shared.dto.PaginationResponse;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final int DEFAULT_PAGE_SIZE = 10;
    private final int MAX_PAGE_SIZE = 30;

    private final ProductService productService;

    public ProductController(ProductService productService){
        this.productService = productService;
    }

    
    // --- Get all products ---
    @GetMapping
    public ResponseEntity<PaginationResponse<PublicProductResponse>> getAllActive(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int pageSize
    ){

        if(pageSize < 1 || pageSize > MAX_PAGE_SIZE){
            throw new IllegalArgumentException(
                "pageSize must be between 1 and " + MAX_PAGE_SIZE
            );
        }

        Pageable pageable = PageRequest.of(page-1, pageSize, Sort.by(Sort.Direction.DESC, "updatedAt"));

        PaginationResponse<PublicProductResponse> products = productService.getAllActive(pageable);
        
        return ResponseEntity.ok(products);
    }


    // --- Product Details by slug ---
    @GetMapping("/{slug}")
    public ResponseEntity<PublicProductDetailsResponse> getProductBySlug(@PathVariable String slug) {

        PublicProductDetailsResponse product = productService.getProductBySlug(slug);

        return ResponseEntity.ok(product);

    }

}
