package com.kartify.api.product.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kartify.api.product.entity.ProductVariant;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    boolean existsBySku(String sku);

    @Query("""
        SELECT DISTINCT variant
        FROM ProductVariant variant
        LEFT JOIN FETCH variant.attributeValues
        WHERE variant.product.id = :productId
    """)
    List<ProductVariant> findByProductIdWithAttributeValues(@Param("productId") Long productId);

}
