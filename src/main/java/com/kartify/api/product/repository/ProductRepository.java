package com.kartify.api.product.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kartify.api.product.entity.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySlug(String slug);

    boolean existsBySku(String sku);

}
