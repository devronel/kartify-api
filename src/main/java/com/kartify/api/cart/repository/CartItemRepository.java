package com.kartify.api.cart.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kartify.api.cart.entity.CartItem;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

  Optional<CartItem> findByCartIdAndProductIdAndProductVariantId(
    Long cartId,
    Long productId,
    Long productVariantId
  );

  Optional<CartItem> findByCartIdAndProductIdAndProductVariantIdIsNull(
    Long cartId,
    Long productId
  );

}
