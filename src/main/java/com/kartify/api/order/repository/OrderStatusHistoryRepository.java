package com.kartify.api.order.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kartify.api.order.entity.OrderStatusHistory;

@Repository
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {

  @Query("""
    SELECT orderStatusHistory FROM OrderStatusHistory orderStatusHistory
    WHERE orderStatusHistory.order.id IN :orderIds
    ORDER BY orderStatusHistory.createdAt DESC
  """)
  List<OrderStatusHistory> findAllDesc(@Param("orderIds") List<Long> orderIds);


  @Query("""
    SELECT osh FROM OrderStatusHistory osh
    WHERE osh.order.id IN :orderIds
      AND osh.createdAt = (
        SELECT MAX(osh2.createdAt) FROM OrderStatusHistory osh2
        WHERE osh2.order = osh.order
      )
  """)
  List<OrderStatusHistory> findAllLatestByOrderIds(@Param("orderIds") List<Long> orderIds);

}
