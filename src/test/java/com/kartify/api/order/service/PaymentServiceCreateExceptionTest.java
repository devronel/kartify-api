package com.kartify.api.order.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kartify.api.exception.ResourceNotFoundException;
import com.kartify.api.order.entity.Payment;
import com.kartify.api.order.repository.OrderRepository;
import com.kartify.api.order.repository.PaymentRepository;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceCreateExceptionTest {

  @Mock
  private PaymentRepository paymentRepository;

  @Mock
  private OrderRepository orderRepository;

  @InjectMocks
  private PaymentService paymentService;

  private Long orderId = 1L;
  private Long paymentId = 1L;
  private String providerPaymentId = "ORD-123";
  private String checkoutUrl = "url";
  private String payload = "payload";

  @Nested
  class CreatePaymentTest{
    
    @Test
    void createOrder_ShouldThrowException_IfOrderNotFound(){

      when(paymentRepository.findByIdAndOrderId(paymentId, orderId))
        .thenReturn(Optional.empty());
  
      Assertions.assertThrows(ResourceNotFoundException.class, () -> {
        paymentService.update(orderId, paymentId, providerPaymentId, checkoutUrl, payload);
      });

      verify(paymentRepository, never()).save(any(Payment.class));
  
    }

  }


}
