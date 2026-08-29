package com.kartify.api.category.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kartify.api.category.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByParentId(Long parentId);

    Optional<Category> findBySlug(String slug);

    @Query("""
        SELECT category FROM 
        Category category 
        WHERE category.parent IS NULL
        ORDER BY category.sortOrder ASC
    """)
    List<Category> findAllTopLevel();

    @Query("""
        SELECT category FROM 
        Category category 
        WHERE category.parent.id = :parentId
        ORDER BY category.sortOrder ASC
    """)
    List<Category> findAllChild(@Param("parentId") Long parentId);

    @Query("SELECT MAX(category.sortOrder) FROM Category category WHERE category.parent IS NULL")
    Integer findMaxSortOrderForTopLevel();

    @Query("SELECT MAX(category.sortOrder) FROM Category category WHERE category.parent.id = :parentId")
    Integer findMaxSortOrderByParentId(@Param("parentId") Long parentId);

}
