package com.kartify.api.category.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.kartify.api.category.dto.CategoryCreateRequest;
import com.kartify.api.category.dto.CategoryResponse;
import com.kartify.api.category.dto.CategoryTreeResponse;
import com.kartify.api.category.dto.CategoryUpdateRequest;
import com.kartify.api.category.entity.Category;
import com.kartify.api.category.repository.CategoryRepository;
import com.kartify.api.exception.FieldValidationException;
import com.kartify.api.exception.ResourceNotFoundException;
import com.kartify.api.shared.helper.SlugUtil;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository){
        this.categoryRepository = categoryRepository;
    }

    // --- Create Category ---
    public CategoryResponse create(CategoryCreateRequest payload){

        Category category = new Category();
        
        if(payload.parentId() != null){
            Category parent = categoryRepository.findById(payload.parentId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

            category.setParent(parent);
        }

        category.setName(payload.name());
        category.setSlug(generateUniquesSlug(payload.name()));
        category.setDescription(payload.description());
        category.setIsActive(payload.isActive());

        // --- Auto sort order ---
        Integer maxSortOrder;
        if(payload.parentId() == null){
            maxSortOrder = categoryRepository.findMaxSortOrderForTopLevel();
        }else{
            Category parent = categoryRepository.findById(payload.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found"));
            category.setParent(parent);
            maxSortOrder = categoryRepository.findMaxSortOrderByParentId(payload.parentId());
        }

        category.setSortOrder(maxSortOrder != null ? maxSortOrder + 1 : 0);

        Category createdCategory = categoryRepository.save(category);

        Long parentId = (createdCategory.getParent() != null) ? createdCategory.getParent().getId() : null;

        return new CategoryResponse(
            createdCategory.getId(),
            parentId,
            createdCategory.getName(),
            createdCategory.getSlug(),
            createdCategory.getDescription(),
            createdCategory.getIsActive(),
            createdCategory.getSortOrder()
        );

    }

    // --- Update Category ---
    public CategoryResponse update(Long id, CategoryUpdateRequest payload){

        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Category not found."));

        // --- Checked to prevent making the parent category itself ---
        if (payload.parentId() != null && payload.parentId().equals(id)) {
            throw new FieldValidationException("parentId", "A category cannot be its own parent.");
        }

        Long oldParentId = category.getParent() != null ? category.getParent().getId() : null;
        boolean parentChanged = !Objects.equals(oldParentId, payload.parentId());

        if (payload.parentId() == null) {
            category.setParent(null);
        } else {
            Category newParent = categoryRepository.findById(payload.parentId())
                .orElseThrow(() -> new ResourceNotFoundException("Parent category not found"));
            category.setParent(newParent);
        }

        category.setName(payload.name());
        category.setSlug(generateUniquesSlug(payload.name()));
        category.setDescription(payload.description());
        category.setIsActive(payload.isActive());

        
        if (parentChanged) {
            Integer maxSortOrder;
            if (payload.parentId() == null) {
                maxSortOrder = categoryRepository.findMaxSortOrderForTopLevel();
            } else if (oldParentId == null) {
                maxSortOrder = categoryRepository.findMaxSortOrderByParentId(payload.parentId());
            } else {
                maxSortOrder = categoryRepository.findMaxSortOrderByParentId(payload.parentId());
            }
            category.setSortOrder(maxSortOrder != null ? maxSortOrder + 1 : 0);
        }

        Category createdCategory = categoryRepository.save(category);

        Long parentId = (createdCategory.getParent() != null) ? createdCategory.getParent().getId() : null;

        return new CategoryResponse(
            createdCategory.getId(),
            parentId,
            createdCategory.getName(),
            createdCategory.getSlug(),
            createdCategory.getDescription(),
            createdCategory.getIsActive(),
            createdCategory.getSortOrder()
        );

    }


    // --- Find all top-level category ---
    public List<CategoryTreeResponse> getTopLevelCategory() {
        return categoryRepository.findAllTopLevel()
            .stream().map(this::toTreeResponse).toList();
    }

    // --- Find all child category ---
    public List<CategoryResponse> getChildCategory(Long id) {

        categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Parent id not found"));

        return categoryRepository.findAllChild(id).stream()
            .map(category -> new CategoryResponse(
                category.getId(),
                category.getParent().getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getIsActive(),
                category.getSortOrder()
            )).toList();

    }

    // --- Create Tree category responsive using recursive ---
    private CategoryTreeResponse toTreeResponse(Category category) {
        List<CategoryTreeResponse> children = category.getChildren().stream()
            .map(this::toTreeResponse)
            .toList();

        return new CategoryTreeResponse(
            category.getId(),
            category.getName(),
            category.getSlug(),
            category.getIsActive(),
            0,
            children
        );
    } 

    // --- Generate unique slug ---
    private String generateUniquesSlug(String name){
        String baseSlug = SlugUtil.toSlug(name);
        String slug = baseSlug;
        int counter = 1;

        while (categoryRepository.findBySlug(slug).isPresent()) {
            slug = baseSlug + "-" + counter;
            counter++;
        }

        return slug;
    }

}
