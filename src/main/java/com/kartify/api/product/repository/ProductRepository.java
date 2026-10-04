package com.kartify.api.product.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kartify.api.product.entity.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findBySlug(String slug);

    boolean existsBySku(String sku);

    @EntityGraph(attributePaths = {"category", "files"})
    @Query("SELECT product FROM Product product")
    List<Product> findAllWithCategoryAndFiles(Pageable pageable);


    //  Get all search products
    @Query("""
        SELECT product
        FROM Product product
        JOIN product.category cat
        WHERE
            :search IS NUll OR :search = '' OR
            LOWER(product.name) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(product.sku) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(cat.name) LIKE LOWER(CONCAT('%', :search, '%'))
    """)
    Page<Product> search(@Param("search") String search, Pageable pageable);


    // Get the filtered, search and active products
    @Query("""
        SELECT product
        FROM Product product
        JOIN product.category cat
        WHERE product.isActive IS TRUE
            AND (:category IS NULL OR :category = '' OR cat.name = :category)
            AND (
                :search IS NULL OR :search = '' OR
                LOWER(product.name) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(product.sku) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(cat.name) LIKE LOWER(CONCAT('%', :search, '%'))
            )
    """)
    Page<Product> searchActive(@Param("search") String search, @Param("category") String category, Pageable pageable);


    // Get all the products equal to category ids
    @Query("""
        SELECT COUNT(product)
        FROM Product product 
        WHERE product.category.id IN :categoryIds
    """)
    Integer countByCategoryIdIn(@Param("categoryIds") List<Long> categoryIds);
}
