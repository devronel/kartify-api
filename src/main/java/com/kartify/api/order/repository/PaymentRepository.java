package com.kartify.api.order.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kartify.api.order.entity.Payment;
import com.kartify.api.order.enums.PaymentStatus;

import jakarta.persistence.LockModeType;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT payment FROM Payment payment WHERE payment.providerPaymentId = :providerPaymentId")
  Optional<Payment> findByProviderPaymentIdForUpdate(@Param("providerPaymentId") String providerPaymentId);


  Optional<Payment> findByIdAndOrderId(Long id, Long orderId);


  Optional<Payment> findFirstByOrderIdOrderByCreatedAtDesc(Long orderId);


  @Query("""
    SELECT payment FROM Payment payment
    WHERE payment.order.id IN :orderIds
    ORDER BY payment.createdAt DESC
  """)
  List<Payment> findAllDesc(@Param("orderIds") List<Long> orderIds);


  List<Payment> findAllByOrderIdOrderByCreatedAtDesc(Long orderId);


  @Query("""
    SELECT payment FROM Payment payment
    WHERE payment.order.id IN :orderIds
      AND payment.createdAt = (
        SELECT MAX(p2.createdAt) FROM Payment p2
        WHERE p2.order = payment.order
      )
  """)
  List<Payment> findAllLatestByOrderIds(@Param("orderIds") List<Long> orderIds);


  List<Payment> findAllByOrderIdAndStatusIn(Long orderIds, Collection<PaymentStatus> statuses);

}
