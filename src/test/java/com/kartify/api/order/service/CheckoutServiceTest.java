package com.kartify.api.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kartify.api.exception.PaymentCreationRejectedException;
import com.kartify.api.exception.PaymentCreationUnknownException;
import com.kartify.api.order.dto.CheckoutResponse;
import com.kartify.api.order.dto.OrderCreateRequest;
import com.kartify.api.order.dto.OrderCreateResponse;
import com.kartify.api.order.dto.OrderPaymentResponse;
import com.kartify.api.order.entity.Payment;
import com.kartify.api.order.enums.PaymentStatus;
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
    String orderNumber = "ORD-2026-UIUHJHKHK";
    BigDecimal totalAmount = new BigDecimal("1500.00");
    String idempotencyKey = "idm_key_UIUIKJKLH";

    Payment payment = new Payment();
    payment.setStatus(PaymentStatus.CREATING);

    OrderCreateResponse order = new OrderCreateResponse(
      orderId,
      orderNumber,
      totalAmount
    );

    OrderPaymentResponse orderPaymentResponse = new OrderPaymentResponse(
      order.orderId(),
      payment.getId(),
      order.orderNumber(),
      order.totalAmount(),
      idempotencyKey
    );

    PaymentLink paymentLink = new PaymentLink(
      "pay_123",
      "https://checkout.example.com/pay_123",
      "payment-payload"
    );

    when(orderService.create(userId, payload))
      .thenReturn(order);

    when(paymentService.create(order.orderId()))
      .thenReturn(payment);

    when(paymongoService.process(any(PaymentLinkRequest.class)))
      .thenReturn(paymentLink);

    // Act
    CheckoutResponse response = checkoutService.placeOrder(userId, payload);

    // Assert
    assertEquals("pay_123", response.orderNumber());
    assertEquals("https://checkout.example.com/pay_123", response.checkoutUrl());
    verify(orderService).create(userId, payload);
    verify(paymentService).create(order.orderId());
    verify(paymongoService).process(any(PaymentLinkRequest.class));
    verify(paymentService).update(
        orderPaymentResponse.paymentId(),
        orderPaymentResponse.orderId(),
        paymentLink.paymentLinkId(),
        paymentLink.checkoutUrl(),
        paymentLink.payload()
    );
  }

  @Test
  void placeOrder_ShouldThrowException_WhenReturnPaymentCreationRejectedException() {

    Long userId = 1L;
    OrderCreateRequest payload = mock(OrderCreateRequest.class);

    Long orderId = 10L;
    String orderNumber = "ORD-2026-UIUHJHKHK";
    BigDecimal totalAmount = new BigDecimal("1500.00");

    Payment payment = new Payment();
    payment.setStatus(PaymentStatus.CREATING);

    Payment updatedPayment = new Payment();
    updatedPayment.setStatus(PaymentStatus.SETUP_FAILED);

    OrderCreateResponse order = new OrderCreateResponse(
      orderId,
      orderNumber,
      totalAmount
    );

    when(orderService.create(userId, payload))
      .thenReturn(order);

    when(paymentService.create(order.orderId()))
      .thenReturn(payment);

    when(paymongoService.process(any(PaymentLinkRequest.class)))
      .thenThrow(PaymentCreationRejectedException.class);

    // Act
    Assertions.assertThrows(PaymentCreationRejectedException.class, () -> {
      checkoutService.placeOrder(userId, payload);
    });
    verify(paymentService).updateStatus(payment.getId(), order.orderId(), PaymentStatus.SETUP_FAILED);

  }


  @Test
  void placeOrder_ShouldThrowException_WhenReturnPaymentCreationUnknownException() {

    Long userId = 1L;
    OrderCreateRequest payload = mock(OrderCreateRequest.class);

    Long orderId = 10L;
    String orderNumber = "ORD-2026-UIUHJHKHK";
    BigDecimal totalAmount = new BigDecimal("1500.00");

    Payment payment = new Payment();
    payment.setStatus(PaymentStatus.CREATING);

    Payment updatedPayment = new Payment();
    updatedPayment.setStatus(PaymentStatus.SETUP_FAILED);

    OrderCreateResponse order = new OrderCreateResponse(
      orderId,
      orderNumber,
      totalAmount
    );

    when(orderService.create(userId, payload))
      .thenReturn(order);

    when(paymentService.create(order.orderId()))
      .thenReturn(payment);

    when(paymongoService.process(any(PaymentLinkRequest.class)))
      .thenThrow(PaymentCreationUnknownException.class);

    // Act
    Assertions.assertThrows(PaymentCreationUnknownException.class, () -> {
      checkoutService.placeOrder(userId, payload);
    });
    verify(paymentService).updateStatus(payment.getId(), order.orderId(), PaymentStatus.CREATION_UNKNOWN);

  }

}
