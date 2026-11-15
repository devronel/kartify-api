package com.kartify.api.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

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
import com.kartify.api.order.dto.OrderPaymentResponse;
import com.kartify.api.order.entity.Payment;
import com.kartify.api.order.enums.PaymentStatus;
import com.kartify.api.payment.contracts.PaymentProvider;
import com.kartify.api.payment.dto.PaymentLink;
import com.kartify.api.payment.dto.PaymentLinkRequest;

@ExtendWith(MockitoExtension.class)
public class CheckoutServiceTest {

  @Mock
  private OrderService orderService;

  @Mock
  private PaymentProvider paymentProvider;

  @Mock
  private PaymentService paymentService;

  @InjectMocks
  private CheckoutService checkoutService;

  private final String idempotencyKey = "idm_key_UIUIKJKLH";

  @Test
  void placeOrder_ShouldSuccess_WhenAllServicesSucceed() {

    // Arrange
    Long userId = 1L;
    OrderCreateRequest payload = mock(OrderCreateRequest.class);

    Long orderId = 10L;
    Long paymentId = 20L;

    String orderNumber = "ORD-2026-UIUHJHKHK";

    BigDecimal totalAmount = new BigDecimal("1500.00");

    OrderPaymentResponse order = new OrderPaymentResponse(
      orderId,
      paymentId,
      orderNumber,
      totalAmount,
      idempotencyKey
    );

    PaymentLink paymentLink = new PaymentLink(
      "pay_123",
      "https://checkout.example.com/pay_123",
      "payment-payload"
    );

    when(paymentService.createOrderAndPayment(userId, payload))
      .thenReturn(order);

    when(paymentProvider.process(any(PaymentLinkRequest.class)))
      .thenReturn(paymentLink);

    // Act
    CheckoutResponse response = checkoutService.placeOrder(userId, payload);

    // Assert
    assertNotNull(response);
    assertEquals(orderNumber, response.orderNumber());
    assertEquals("https://checkout.example.com/pay_123", response.checkoutUrl());

    verify(paymentService).createOrderAndPayment(userId, payload);

    verify(paymentProvider).process(any(PaymentLinkRequest.class));

    verify(paymentService).update(
      paymentId,
      orderId,
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

    OrderPaymentResponse order = new OrderPaymentResponse(
      orderId,
      payment.getId(),
      orderNumber,
      totalAmount,
      idempotencyKey
    );

    when(paymentService.createOrderAndPayment(userId, payload))
      .thenReturn(order);

    when(paymentProvider.process(any(PaymentLinkRequest.class)))
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

    OrderPaymentResponse order = new OrderPaymentResponse(
      orderId,
      payment.getId(),
      orderNumber,
      totalAmount,
      idempotencyKey
    );

    when(paymentService.createOrderAndPayment(userId, payload))
      .thenReturn(order);

    when(paymentProvider.process(any(PaymentLinkRequest.class)))
      .thenThrow(PaymentCreationUnknownException.class);

    // Act
    Assertions.assertThrows(PaymentCreationUnknownException.class, () -> {
      checkoutService.placeOrder(userId, payload);
    });
    verify(paymentService).updateStatus(payment.getId(), order.orderId(), PaymentStatus.CREATION_UNKNOWN);

  }

}
