package com.kartify.api.product.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kartify.api.product.entity.ProductFile;

@Repository
public interface ProductFileRepository extends JpaRepository<ProductFile, Long> {

    Optional<ProductFile> findByIdAndProductId(Long id, Long productId);

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


    @Modifying
    @Query("DELETE FROM ProductFile file WHERE file.id IN :ids")
    void deleteByIds(@Param("ids") Collection<Long> ids);

}
