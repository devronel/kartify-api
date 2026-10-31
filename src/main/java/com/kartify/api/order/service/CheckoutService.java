package com.kartify.api.order.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

@Service
public class CheckoutService {

  
  private final OrderService orderService;
  private final PaymongoService paymongoService;
  private final PaymentService paymentService;


  public CheckoutService(
    OrderService orderService,
    PaymongoService paymongoService,
    PaymentService paymentService
  ){
    this.orderService = orderService;
    this.paymongoService = paymongoService;
    this.paymentService = paymentService;
  }


  // Process and Pay the Order
  public CheckoutResponse placeOrder(Long userId,  OrderCreateRequest payload){

    OrderPaymentResponse order = createOrderAndPayment(userId, payload);

    try {

      PaymentLink paymentProvider = paymongoService.process(new PaymentLinkRequest(
        order.totalAmount(),
        "Payment for Order " + order.orderNumber(),
        "Order Number: " + order.orderNumber()
      ));
  
      paymentService.update(order.paymentId(), order.orderId(), paymentProvider.paymentLinkId(), paymentProvider.checkoutUrl(), paymentProvider.payload());
  
      return new CheckoutResponse(
        paymentProvider.paymentLinkId(), 
        paymentProvider.checkoutUrl()
      );
      
    } catch (PaymentCreationRejectedException e) {
      paymentService.updateStatus(order.paymentId(), order.orderId(), PaymentStatus.SETUP_FAILED);
      throw e;
    } catch (PaymentCreationUnknownException e) {
      paymentService.updateStatus(order.paymentId(), order.orderId(), PaymentStatus.CREATION_UNKNOWN);
      throw e;
    }

  }


  // Create order and payment transactions
  @Transactional
  public OrderPaymentResponse createOrderAndPayment(Long userId,  OrderCreateRequest payload){

    OrderCreateResponse order = orderService.create(userId, payload);

    Payment payment = paymentService.create(order.orderId());

    return new OrderPaymentResponse(
      order.orderId(),
      payment.getId(),
      order.orderNumber(),
      order.totalAmount()
    );
    
  }

}
