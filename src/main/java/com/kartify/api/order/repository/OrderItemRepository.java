package com.kartify.api.order.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kartify.api.order.entity.OrderItem;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

  // Find all the items using order ids
  @Query("""
    SELECT orderItem
    FROM OrderItem orderItem
    WHERE orderItem.order.id IN :orderIds
  """)
  List<OrderItem> findAllByOrderIds(@Param("orderIds") List<Long> orderIds);
}
