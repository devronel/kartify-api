package com.kartify.api.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kartify.api.order.entity.Payment;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

}
