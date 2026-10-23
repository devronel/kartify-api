package com.kartify.api.order.service;

import org.springframework.stereotype.Service;

import com.kartify.api.order.dto.CheckoutResponse;
import com.kartify.api.order.dto.OrderCreateRequest;
import com.kartify.api.order.dto.OrderCreateResponse;
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
    
    OrderCreateResponse order = orderService.create(userId, payload);

    PaymentLink payment = paymongoService.process(new PaymentLinkRequest(
      order.totalAmount(),
      "Payment",
      "Paid"
    ));

    paymentService.create(order.orderId(), payment.id(), payment.checkoutUrl(), payment.payload());

    return new CheckoutResponse(
      payment.id(), 
      payment.checkoutUrl()
    );

  }

}
