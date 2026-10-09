package com.kartify.api.cart.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

  Page<CartItem> findAllByCartId(Long cartId, Pageable pageable);

  List<CartItem> findAllByCartIdOrderByCreatedAtDesc(Long cartId);

}
