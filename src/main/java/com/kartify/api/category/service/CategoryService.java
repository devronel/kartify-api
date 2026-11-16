package com.kartify.api.category.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(CategoryService.class);

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository){
        this.categoryRepository = categoryRepository;
    }


    /**
     * Creates a new category and assigns its sort order based on its parent.
     *
     * If a parent category is provided, the new category will be created
     * as a child of that category. Otherwise, it will be created as a
     * top-level category.
     *
     * @param payload the category creation details
     * @return the created category response
     * @throws ResourceNotFoundException if the specified parent category does not exist
    */
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

        return toResponse(createdCategory);

    }


    /**
     * Updates an existing category, including its parent relationship and sort order.
     *
     * Prevents a category from being assigned as its own parent or as the parent
     * of one of its immediate children.
     *
     * Updates the sort order when the category is moved to a different parent.
     *
     * @param id the ID of the category to update
     * @param payload the updated category details
     * @return the updated category response
     * @throws ResourceNotFoundException if the category or specified parent does not exist
     * @throws FieldValidationException if the category is assigned as its own parent or as the parent of one of its children
     */
    public CategoryResponse update(Long id, CategoryUpdateRequest payload){

        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Category not found."));

        // Prevent circular parent relationships.
        if (payload.parentId() != null) {

            // A category cannot be its own parent.
            if (payload.parentId().equals(id)) {
                throw new FieldValidationException("parentId", "A category cannot be its own parent.");
            }

            // An immediate child cannot become its parent's parent.
            boolean isChild = category.getChildren().stream()
                .anyMatch(child -> child.getId().equals(payload.parentId()));

            if (isChild) {
                throw new FieldValidationException("parentId", "A category cannot have its child as its parent.");
            }
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
        
        if(!payload.name().equals(category.getName())){
            category.setSlug(generateUniquesSlug(payload.name()));
        }
        category.setName(payload.name());
        category.setDescription(payload.description());
        category.setIsActive(payload.isActive());

        
        // Assign the next available sort order when the parent changes.
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

        return toResponse(createdCategory);

    }


    /**
     * Retrieves a category by its ID.
     *
     * @param id the category ID
     * @return the category response
     * @throws ResourceNotFoundException if the category does not exist
     */
    public CategoryResponse findCategoryById(Long id){        
        Category category = categoryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        return toResponse(category);
    }


    /**
     * Find all categories
     *
     * @return List of categories and its child
    */
    public List<CategoryTreeResponse> findTopLevelCategory() {
        return categoryRepository.findAllTopLevel().stream().map(this::toTreeResponse).toList();
    }


    // -------------------- Helper -------------------------------

    public List<Long> collectCategoryAndChildIds(Category category) {
        List<Long> ids = new ArrayList<>();
        
        ids.add(category.getId());

        for (Category child : category.getChildren()) {
            ids.addAll(collectCategoryAndChildIds(child));
        }

        return ids;
    }
    

    private CategoryTreeResponse toTreeResponse(Category category) {
        List<CategoryTreeResponse> children = category.getChildren().stream()
            .map(this::toTreeResponse)
            .toList();

        return new CategoryTreeResponse(
            category.getId(),
            category.getParent() != null ? category.getParent().getId() : null,
            category.getName(),
            category.getSlug(),
            category.getIsActive(),
            0,
            children
        );
    } 


    private CategoryResponse toResponse(Category category) {
        CategoryResponse parent = category.getParent() != null
            ? toParentResponse(category.getParent())
            : null;

        return new CategoryResponse(
            category.getId(),
            parent,
            category.getName(),
            category.getSlug(),
            category.getDescription(),
            category.getIsActive(),
            category.getSortOrder()
        );
    }


    private CategoryResponse toParentResponse(Category category) {
        return new CategoryResponse(
            category.getId(),
            null,
            category.getName(),
            category.getSlug(),
            category.getDescription(),
            category.getIsActive(),
            category.getSortOrder()
        );
    }


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

    // -------------------- Helper -------------------------------

}
