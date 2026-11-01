package com.kartify.api.order.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kartify.api.exception.ResourceNotFoundException;
import com.kartify.api.order.dto.PaymentAttemptResponse;
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

    String idempotencyKey = "idm_key_" + UUID.randomUUID().toString().replace("-", "");

    Payment payment = new Payment();
    payment.setOrder(order);
    payment.setProvider("Paymongo");
    payment.setPaymentMethod(PaymentMethod.QRPh);
    payment.setStatus(PaymentStatus.CREATING);
    payment.setAmount(order.getTotal());
    payment.setCurrency("PHP");
    payment.setIdempotencyKey(idempotencyKey);

    Payment paymentCreated = paymentRepository.save(payment);

    return paymentCreated;

  }


  // Create attemp for payment record
  public Payment createAttempt(Order order, String idempotencyKey){

    Payment payment = new Payment();

    payment.setOrder(order);
    payment.setIdempotencyKey(idempotencyKey);
    payment.setStatus(PaymentStatus.CREATING);

    Payment savedPayment = paymentRepository.save(payment);

    return savedPayment;

  }


  @Transactional
  public void completePaymentSetup(Long paymentId, String providerPaymentId, String checkoutUrl, String payload) {

    Payment payment = paymentRepository.findById(paymentId)
      .orElseThrow(() -> new RuntimeException("Payment not found"));

    payment.setProviderPaymentId(providerPaymentId);
    payment.setCheckoutUrl(checkoutUrl);
    payment.setPayload(payload);
    payment.setStatus(PaymentStatus.PENDING);

    paymentRepository.save(payment);

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


  // Mark payment PAID
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


  // Prepare retry payment
  @Transactional
  public PaymentAttemptResponse prepareRetry(Long userId, Long orderId){

    Order order = orderRepository.findByIdAndUseridForUpdate(orderId, userId)
      .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

    Payment payment = paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(order.getId())
      .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));


    if(payment.getStatus() == PaymentStatus.SETUP_FAILED){

      String idempotencyKey = "idm_key_" + UUID.randomUUID().toString().replace("-", "");

      Payment newPayment = createAttempt(order, idempotencyKey);

      return new PaymentAttemptResponse(
        order.getId(),
        newPayment.getId(),
        newPayment.getIdempotencyKey(),
        order.getTotal(),
        order.getOrderNumber()
      );

    } else if(payment.getStatus() == PaymentStatus.CREATION_UNKNOWN){

      return new PaymentAttemptResponse(
        order.getId(),
        payment.getId(),
        payment.getIdempotencyKey(),
        order.getTotal(),
        order.getOrderNumber()
      );

    } else if (payment.getStatus() == PaymentStatus.SUCCEEDED) {

      throw new IllegalStateException("Order has already been paid.");

    } else if (payment.getStatus() == PaymentStatus.CREATING) {

      throw new IllegalStateException("Payment creation is already in progress.");

    }

    throw new IllegalStateException("Payment cannot be retried.");

  }
}
