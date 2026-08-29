package com.kartify.api.category.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kartify.api.category.dto.CategoryCreateRequest;
import com.kartify.api.category.dto.CategoryResponse;
import com.kartify.api.category.dto.CategoryUpdateRequest;
import com.kartify.api.category.service.CategoryService;
import com.kartify.api.shared.dto.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/product/category")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService){
        this.categoryService = categoryService;
    }

    // --- Create Category endpoint ---
    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> create(@Valid @RequestBody CategoryCreateRequest request){
        CategoryResponse createdCategory = categoryService.create(request);
        return ResponseEntity.ok(ApiResponse.success("Category Created", createdCategory));
    }

    // --- Update Category endpoint ---
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> update(
        @PathVariable Long id,
        @Valid @RequestBody CategoryUpdateRequest request
    ){
        CategoryResponse updatedCategory = categoryService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Category Updated", updatedCategory));
    }

}
