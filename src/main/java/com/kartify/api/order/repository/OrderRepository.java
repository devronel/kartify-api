package com.kartify.api.order.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kartify.api.order.entity.Order;

import jakarta.persistence.LockModeType;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

  // Get order by id
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT order FROM Order order WHERE order.id = :id")
  Optional<Order> findByIdForUpdate(@Param("id") Long id); 


  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT order FROM Order order WHERE order.id = :id AND order.user.id = :userId")
  Optional<Order> findByIdAndUseridForUpdate(@Param("id") Long id, @Param("userId") Long userId); 


  // Get all order by userId
  Page<Order> findByUserId(Long userId, Pageable pageable);

  Optional<Order> findByIdAndUserId(Long id, Long userId);

}
