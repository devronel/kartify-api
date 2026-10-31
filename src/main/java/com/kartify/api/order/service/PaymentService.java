package com.kartify.api.order.service;

import java.math.BigDecimal;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kartify.api.exception.ResourceNotFoundException;
import com.kartify.api.order.entity.Order;
import com.kartify.api.order.entity.Payment;
import com.kartify.api.order.enums.OrderStatus;
import com.kartify.api.order.enums.PaymentMethod;
import com.kartify.api.order.enums.PaymentStatus;
import com.kartify.api.order.repository.OrderRepository;
import com.kartify.api.order.repository.PaymentRepository;

@Service
public class PaymentService {

  private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

  private final PaymentRepository paymentRepository;
  private final OrderRepository orderRepository;
  private final OrderService orderService;

  public PaymentService(
    PaymentRepository paymentRepository,
    OrderRepository orderRepository,
    OrderService orderService
  ){
    this.paymentRepository = paymentRepository;
    this.orderRepository = orderRepository;
    this.orderService = orderService;
  }


  // Create Payment
  public Payment create(Long orderId){

    Order order = orderRepository.findById(orderId)
      .orElseThrow(() -> new ResourceNotFoundException("Order not found."));

    Payment payment = new Payment();
    payment.setOrder(order);
    payment.setProvider("Paymongo");
    payment.setPaymentMethod(PaymentMethod.QRPh);
    payment.setStatus(PaymentStatus.CREATING);
    payment.setAmount(order.getTotal());
    payment.setCurrency("PHP");

    Payment paymentCreated = paymentRepository.save(payment);

    return paymentCreated;

  }


  // Update Payment
  @Transactional
  public void update(Long paymentId, Long orderId, String paymentLinkId, String checkoutUrl, String payload){

    Payment payment = paymentRepository.findByIdAndOrderId(paymentId, orderId)
      .orElseThrow(() -> new ResourceNotFoundException("Payment not found."));

    payment.setProviderPaymentId(paymentLinkId);
    payment.setCheckoutUrl(checkoutUrl);
    payment.setPayload(payload);
    payment.setStatus(PaymentStatus.PENDING);

  }


  // Update status of payment
  @Transactional
  public void updateStatus(Long paymentId, Long orderId, PaymentStatus status){

    Payment payment = paymentRepository.findByIdAndOrderId(paymentId, orderId)
      .orElseThrow(() -> new ResourceNotFoundException("Payment not found."));

    payment.setStatus(status);

  }


  @Transactional
  public void markPaid(String paymentLinkId, BigDecimal paidAmount, Instant paidAt, String rawBody){

    Payment payment = paymentRepository.findByProviderPaymentIdForUpdate(paymentLinkId).orElse(null);
    
    if(payment == null){
      log.warn("Webhook for unknown payment link {}", paymentLinkId);
      return;
    }

    // To avoid duplicate
    if(payment.getStatus() == PaymentStatus.SUCCEEDED){
      return;
    }

    // If Payment mismatch from payment table to the return paymongo payment
    if(payment.getAmount().compareTo(paidAmount) != 0){
      log.error("Amount mismatch on payment {}: expected {}, got {}", payment.getId(), payment.getAmount(), paidAmount);
      return;
    }

    payment.setStatus(PaymentStatus.SUCCEEDED);
    payment.setPaidAt(paidAt);
    payment.setPayload(rawBody);

    Order order = orderRepository.findByIdForUpdate(payment.getOrder().getId()).orElseThrow();

    if(order.getStatus() == OrderStatus.PENDING){
      orderService.changeStatus(order, OrderStatus.PAID, "Payment Recieved", null);
    }else{
      log.warn("Payment {} succeeded but order {}  is {}: needs a manual refund", payment.getId(), order.getOrderNumber(), order.getStatus());
    }

  }

}
