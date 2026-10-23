package com.kartify.api.order.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kartify.api.exception.ResourceNotFoundException;
import com.kartify.api.order.entity.Order;
import com.kartify.api.order.entity.Payment;
import com.kartify.api.order.enums.PaymentMethod;
import com.kartify.api.order.enums.PaymentStatus;
import com.kartify.api.order.repository.OrderRepository;
import com.kartify.api.order.repository.PaymentRepository;

@Service
public class PaymentService {

  private final PaymentRepository paymentRepository;
  private final OrderRepository orderRepository;

  public PaymentService(
    PaymentRepository paymentRepository,
    OrderRepository orderRepository
  ){
    this.paymentRepository = paymentRepository;
    this.orderRepository = orderRepository;
  }

  @Transactional
  public void create(Long orderId, String providerPaymentId, String checkoutUrl, String payload){

    Order order = orderRepository.findById(orderId)
      .orElseThrow(() -> new ResourceNotFoundException("Order not found."));

    Payment payment = new Payment();
    payment.setOrder(order);
    payment.setProvider("Paymongo");
    payment.setPaymentMethod(PaymentMethod.QRPh);
    payment.setProviderPaymentId(providerPaymentId);
    payment.setCheckoutUrl(checkoutUrl);
    payment.setStatus(PaymentStatus.PENDING);
    payment.setAmount(order.getTotal());
    payment.setCurrency("PHP");
    payment.setPayload(payload);

    paymentRepository.save(payment);

  }

}
