package com.kartify.api.order.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kartify.api.order.entity.Order;
import com.kartify.api.order.entity.Payment;
import com.kartify.api.order.repository.OrderRepository;
import com.kartify.api.order.repository.PaymentRepository;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceCreateSuccessfulTest {

  @Mock
  private PaymentRepository paymentRepository;

  @Mock
  private OrderRepository orderRepository;

  @InjectMocks
  private PaymentService paymentService;

  private Long orderId = 1L;
  private String providerPaymentId = "ORD-123";
  private String checkoutUrl = "url";
  private String payload = "payload";

  @Nested
  class CreatePaymentTest{
    
    @Test
    void createOrder_ShouldSuccess_IfOrderIsFound(){
  
      Order order = new Order();
      Payment paymentCreated = new Payment();

      when(orderRepository.findById(orderId))
        .thenReturn(Optional.of(order));

      when(paymentRepository.save(any(Payment.class)))
        .thenReturn(paymentCreated);

      paymentService.create(orderId, providerPaymentId, checkoutUrl, payload);

      verify(paymentRepository, times(1)).save(any(Payment.class));
  
    }

  }

}
