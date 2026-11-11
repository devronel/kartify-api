package com.kartify.api.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kartify.api.order.dto.PaymentAttemptResponse;
import com.kartify.api.order.entity.Order;
import com.kartify.api.order.entity.Payment;
import com.kartify.api.order.enums.PaymentStatus;
import com.kartify.api.order.repository.OrderRepository;
import com.kartify.api.order.repository.PaymentRepository;

@ExtendWith(MockitoExtension.class)
public class PaymentServicePrepareRetryTest {

  @Mock
  private OrderRepository orderRepository;

  @Mock
  private PaymentRepository paymentRepository;

  @InjectMocks
  private PaymentService paymentService;

  private final String orderNumber = "ORD_3434";

  @Test
  void prepareRetry_ShouldReturnPaymentAttemptResponse_IfTheStatusIsSetupFailed(){

    Long userId = 1L;

    Order order = new Order();

    String oldIdempotencyKey = "idm_key_old";
    Payment payment = new Payment();
    payment.setOrder(order);
    payment.setStatus(PaymentStatus.SETUP_FAILED);
    payment.setIdempotencyKey(oldIdempotencyKey);

    String newIdempotencyKey = "idm_key_new";
    Payment newPayment = new Payment();
    newPayment.setOrder(order);
    newPayment.setStatus(PaymentStatus.CREATING);
    newPayment.setIdempotencyKey(newIdempotencyKey);

    when(orderRepository.findByOrderNumberAndUseridForUpdate(orderNumber, userId))
      .thenReturn(Optional.of(order));

    when(paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(order.getId()))
      .thenReturn(Optional.of(payment));

    when(paymentRepository.save(any(Payment.class)))
      .thenReturn(newPayment);

    PaymentAttemptResponse response = paymentService.prepareRetry(userId, orderNumber);

    assertNotNull(response);
    assertEquals(order.getId(), response.orderId());
    assertEquals(newPayment.getId(), response.paymentId());
    assertEquals(newPayment.getIdempotencyKey(), response.idempotencyKey());
    assertEquals(newPayment.getStatus(), PaymentStatus.CREATING);

  }


  @Test
  void prepareRetry_ShouldReturnPaymentAttemptResponse_IfTheStatusIsCreationUnknown(){

    Long userId = 1L;

    Order order = new Order();

    String oldIdempotencyKey = "idm_key_old";
    Payment payment = new Payment();
    payment.setOrder(order);
    payment.setStatus(PaymentStatus.CREATION_UNKNOWN);
    payment.setIdempotencyKey(oldIdempotencyKey);

    when(orderRepository.findByOrderNumberAndUseridForUpdate(orderNumber, userId))
      .thenReturn(Optional.of(order));

    when(paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(order.getId()))
      .thenReturn(Optional.of(payment));

    PaymentAttemptResponse response = paymentService.prepareRetry(userId, orderNumber);

    assertNotNull(response);
    assertEquals(order.getId(), response.orderId());
    assertEquals(payment.getId(), response.paymentId());
    assertEquals(payment.getIdempotencyKey(), response.idempotencyKey());
    assertEquals(payment.getStatus(), PaymentStatus.CREATION_UNKNOWN);

  }


  @Test
  void prepareRetry_ShouldThrowException_IfPaymentStatusIsSucceeded(){

    Long userId = 1L;

    Order order = new Order();

    Payment payment = new Payment();
    payment.setOrder(order);
    payment.setStatus(PaymentStatus.SUCCEEDED);

    when(orderRepository.findByOrderNumberAndUseridForUpdate(orderNumber, userId))
      .thenReturn(Optional.of(order));

    when(paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(order.getId()))
      .thenReturn(Optional.of(payment));

    Assertions.assertThrows(IllegalStateException.class, () -> {
      paymentService.prepareRetry(userId, orderNumber);
    });


  }


  @Test
  void prepareRetry_ShouldThrowException_IfPaymentStatusIsCreating(){

    Long userId = 1L;

    Order order = new Order();

    Payment payment = new Payment();
    payment.setOrder(order);
    payment.setStatus(PaymentStatus.CREATING);

    when(orderRepository.findByOrderNumberAndUserId(orderNumber, userId))
      .thenReturn(Optional.of(order));

    when(paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(order.getId()))
      .thenReturn(Optional.of(payment));

    Assertions.assertThrows(IllegalStateException.class, () -> {
      paymentService.prepareRetry(userId, orderNumber);
    });

  }

}
