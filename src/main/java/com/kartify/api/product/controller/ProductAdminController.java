package com.kartify.api.product.controller;

import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kartify.api.product.dto.ProductAdminListResponse;
import com.kartify.api.product.dto.ProductCreateRequest;
import com.kartify.api.product.dto.ProductEditResponse;
import com.kartify.api.product.dto.ProductResponse;
import com.kartify.api.product.dto.ProductUpdateRequest;
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


    // --- Add new product ---
    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> create(@Valid @ModelAttribute ProductCreateRequest request){
        ProductResponse product = productService.create(request);
        return ResponseEntity.ok(ApiResponse.success("Product is Successfully Created.", product));
    }


    // --- Get all products with pagination ---
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

        Pageable pageable = PageRequest.of(page-1, pageSize, Sort.by(Sort.Direction.DESC, "updatedAt"));

        PaginationResponse<ProductAdminListResponse> products = productService.getAll(search, pageable);
        
        return ResponseEntity.ok(products);
    }


    // --- Get Product Details by id ---
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductEditResponse>> getById(@PathVariable Long id) {

        ProductEditResponse productEditResponse = productService.getById(id);

        return ResponseEntity.ok(ApiResponse.success("Get product!", productEditResponse));
    }


    // --- Update product ---
    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @Valid @ModelAttribute ProductUpdateRequest payload) {

        boolean isUpdated = productService.update(id, payload);

        if(!isUpdated){
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }
    

    // --- Delete product ---
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id){

        productService.delete(id);

        return ResponseEntity.noContent().build();

    }

    
    // --- Toggle product active ---
    @PatchMapping("/{id}/active")
    public ResponseEntity<Void> updateActive(
        @PathVariable Long id,
        @RequestBody Map<String, Boolean> body
    ){

        Boolean active = body.get("active");

        productService.updateActive(id, active);

        return ResponseEntity.noContent().build();

    }

}
