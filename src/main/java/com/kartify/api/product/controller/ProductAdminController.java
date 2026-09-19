package com.kartify.api.product.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kartify.api.product.dto.ProductAdminListResponse;
import com.kartify.api.product.dto.ProductCreateRequest;
import com.kartify.api.product.dto.ProductResponse;
import com.kartify.api.product.service.ProductService;
import com.kartify.api.shared.dto.ApiResponse;
import com.kartify.api.shared.dto.PaginationResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/product")
public class ProductAdminController {

    private final int DEFAULT_PAGE_SIZE = 10;
    private final int MAX_PAGE_SIZE = 30;

    private final ProductService productService;

    public ProductAdminController(ProductService productService){
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> create(@Valid @ModelAttribute ProductCreateRequest request){
        ProductResponse product = productService.create(request);
        return ResponseEntity.ok(ApiResponse.success("Product is Successfully Created.", product));
    }

    @GetMapping
    public ResponseEntity<PaginationResponse<ProductAdminListResponse>> getAll(
        @RequestParam(required = false) String search,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int pageSize
    ){

        if(pageSize < 1 || pageSize > MAX_PAGE_SIZE){
            throw new IllegalArgumentException(
                "pageSize must be between 1 and " + MAX_PAGE_SIZE
            );
        }

        Pageable pageable = PageRequest.of(page-1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        PaginationResponse<ProductAdminListResponse> products = productService.getAll(search, pageable);
        
        return ResponseEntity.ok(products);
    }

}
