package com.kartify.api.product.repository;

import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.kartify.api.product.entity.Product;

public class ProductSpecifications {

    public static Specification<Product> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get("isActive"));
    }

    public static Specification<Product> keyword(String search) {
        return (root, query, cb) -> {
            String like = "%" + search.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("name")), like),
                cb.like(cb.lower(root.get("sku")), like),
                cb.like(cb.lower(root.join("category").get("name")), like)
            );
        };
    }

    public static Specification<Product> inCategory(String category) {
        return (root, query, cb) ->
            cb.equal(root.get("category").get("name"), category);
    }

    public static Specification<Product> hasCategories(List<Long> categoryIds) {
        return (root, query, cb) ->
            root.get("category").get("id").in(categoryIds);
    }

}
