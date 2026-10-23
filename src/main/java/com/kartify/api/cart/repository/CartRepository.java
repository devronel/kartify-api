package com.kartify.api.cart.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kartify.api.cart.entity.Cart;

import jakarta.persistence.LockModeType;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

  Optional<Cart> findByUserId(Long userId);

  // Lock table per user to avoid double request
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT cart FROM Cart cart WHERE cart.user.id = :userId")
  Optional<Cart> findByUserIdForUpdate(@Param("userId") Long userId);

}
