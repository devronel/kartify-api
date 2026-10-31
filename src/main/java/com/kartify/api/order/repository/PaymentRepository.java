package com.kartify.api.order.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kartify.api.order.entity.Payment;

import jakarta.persistence.LockModeType;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT payment FROM Payment payment WHERE payment.providerPaymentId = :providerPaymentId")
  Optional<Payment> findByProviderPaymentIdForUpdate(@Param("providerPaymentId") String providerPaymentId);


  Optional<Payment> findByIdAndOrderId(Long id, Long orderId);

}
