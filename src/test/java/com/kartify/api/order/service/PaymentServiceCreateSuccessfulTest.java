package com.kartify.api.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kartify.api.order.entity.Order;
import com.kartify.api.order.entity.Payment;
import com.kartify.api.order.enums.OrderStatus;
import com.kartify.api.order.enums.PaymentStatus;
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

  @Mock
  private OrderService orderService;

  private Long orderId = 1L;
  private String providerPaymentId = "ORD-123";
  private String checkoutUrl = "url";
  private String payload = "payload";

  @Nested
  class CreatePaymentTest{
    
    @Test
    void createPayment_ShouldSuccess_IfOrderIsFound(){
  
      Order order = new Order();
      Payment paymentCreated = new Payment();

      when(orderRepository.findById(orderId))
        .thenReturn(Optional.of(order));

      when(paymentRepository.save(any(Payment.class)))
        .thenReturn(paymentCreated);

      paymentService.create(orderId, providerPaymentId, checkoutUrl, payload);

      verify(paymentRepository, times(1)).save(any(Payment.class));
  
    }


    @Test
    void markPaymentAsPaid_AmountMismatch_DoesNotSavePayment(){
      
      String linkId = "paid_001";
      BigDecimal paidAmount = BigDecimal.valueOf(100);
      Instant paidAt = Instant.parse("2026-10-25T18:00:00Z");
      String rawBody = "{data: {}}";

      Payment payment = new Payment();
      payment.setStatus(PaymentStatus.PENDING);
      payment.setAmount(BigDecimal.valueOf(200));

      when(paymentRepository.findByProviderPaymentIdForUpdate(linkId))
        .thenReturn(Optional.of(payment));

      paymentService.markPaid(linkId, paidAmount, paidAt, rawBody);

      assertEquals(PaymentStatus.PENDING, payment.getStatus());
      assertNull(payment.getPaidAt());
      verify(paymentRepository, never()).save(any()); 

    }


    @Test
    void markPaymentAsPaid_AmountMatch_SavePayment(){
      
      String linkId = "paid_001";
      BigDecimal paidAmount = BigDecimal.valueOf(100);
      Instant paidAt = Instant.parse("2026-10-25T18:00:00Z");
      String rawBody = "{data: {}}";

      Order order = new Order();
      order.setStatus(OrderStatus.PENDING);

      Payment payment = new Payment();
      payment.setOrder(order);
      payment.setStatus(PaymentStatus.PENDING);
      payment.setAmount(BigDecimal.valueOf(100));

      when(paymentRepository.findByProviderPaymentIdForUpdate(linkId))
        .thenReturn(Optional.of(payment));

      when(orderRepository.findByIdForUpdate(payment.getOrder().getId()))
        .thenReturn(Optional.of(order));

      paymentService.markPaid(linkId, paidAmount, paidAt, rawBody);

      Assertions.assertEquals(PaymentStatus.SUCCEEDED, payment.getStatus());
      Assertions.assertNotNull(payment.getPaidAt());
      Assertions.assertNotNull(payment.getPayload());
      verify(orderService).changeStatus(order, OrderStatus.PAID, "Payment Recieved", null);

    }

  }

}
