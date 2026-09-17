package com.kartify.api.product.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kartify.api.product.entity.ProductFile;

@Repository
public interface ProductFileRepository extends JpaRepository<ProductFile, Long> {

    @Query("""
        SELECT file
        FROM ProductFile file
        WHERE file.product.id IN :productIds
    """)
    List<ProductFile> findAllByProductIds(@Param("productIds") List<Long> productIds);


    @Query("""
        SELECT file
        FROM ProductFile file
        WHERE file.product.id IN :productIds
        AND file.isPrimary = true
    """)
    List<ProductFile> findPrimaryImagesByProductIds(@Param("productIds") List<Long> productIds);

}
