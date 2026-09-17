package com.kartify.api.product.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.kartify.api.product.entity.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySlug(String slug);

    boolean existsBySku(String sku);

    // @Query("""
    //     SELECT product
    //     FROM Product product
    //     JOIN FETCH product.category
    //     LEFT JOIN FETCH product.files
    // """)
    @EntityGraph(attributePaths = {"category", "files"})
    @Query("SELECT product FROM Product product")
    List<Product> findAllWithCategoryAndFiles(Pageable pageable);

}
