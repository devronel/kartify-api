package com.kartify.api.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kartify.api.product.entity.ProductAttribute;

public interface ProductAttributeRepository extends JpaRepository<ProductAttribute, Long> {

}
