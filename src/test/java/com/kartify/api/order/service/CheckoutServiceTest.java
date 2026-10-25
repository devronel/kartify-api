package com.kartify.api.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kartify.api.order.dto.CheckoutResponse;
import com.kartify.api.order.dto.OrderCreateRequest;
import com.kartify.api.order.dto.OrderCreateResponse;
import com.kartify.api.payment.dto.PaymentLink;
import com.kartify.api.payment.dto.PaymentLinkRequest;
import com.kartify.api.payment.service.PaymongoService;

@ExtendWith(MockitoExtension.class)
public class CheckoutServiceTest {

  @Mock
  private OrderService orderService;

  @Mock
  private PaymongoService paymongoService;

  @Mock
  private PaymentService paymentService;

  @InjectMocks
  private CheckoutService checkoutService;

  @Test
  void placeOrder_ShouldSuccess_WhenAllServicesSucceed() {

    Long userId = 1L;
    OrderCreateRequest payload = mock(OrderCreateRequest.class);

    Long orderId = 10L;
    BigDecimal totalAmount = new BigDecimal("1500.00");

    OrderCreateResponse order = new OrderCreateResponse(
      orderId,
      totalAmount
    );

    PaymentLink payment = new PaymentLink(
      "pay_123",
      "https://checkout.example.com/pay_123",
      "payment-payload"
    );

    when(orderService.create(userId, payload))
      .thenReturn(order);

    when(paymongoService.process(any(PaymentLinkRequest.class)))
      .thenReturn(payment);

    // Act
    CheckoutResponse response = checkoutService.placeOrder(userId, payload);

    // Assert
    assertEquals("pay_123", response.orderNumber());
    assertEquals("https://checkout.example.com/pay_123", response.checkoutUrl());

    verify(orderService).create(userId, payload);

    verify(paymongoService).process(any(PaymentLinkRequest.class));

    verify(paymentService).create(
        orderId,
        "pay_123",
        "https://checkout.example.com/pay_123",
        "payment-payload"
    );
  }

}
