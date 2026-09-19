package com.kartify.api.product.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kartify.api.product.entity.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySlug(String slug);

    boolean existsBySku(String sku);

    @EntityGraph(attributePaths = {"category", "files"})
    @Query("SELECT product FROM Product product")
    List<Product> findAllWithCategoryAndFiles(Pageable pageable);

    @Query("""
        SELECT product
        FROM Product product
        JOIN product.category cat
        WHERE
            LOWER(product.name) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(product.sku) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(cat.name) LIKE LOWER(CONCAT('%', :search, '%'))
    """)
    Page<Product> search(@Param("search") String search, Pageable pageable);

}
