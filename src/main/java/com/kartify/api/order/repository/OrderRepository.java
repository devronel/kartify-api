package com.kartify.api.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kartify.api.order.entity.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

}
