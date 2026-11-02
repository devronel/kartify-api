package com.kartify.api.order.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.kartify.api.exception.PaymentCreationRejectedException;
import com.kartify.api.exception.PaymentCreationUnknownException;
import com.kartify.api.order.dto.CheckoutResponse;
import com.kartify.api.order.dto.OrderCreateRequest;
import com.kartify.api.order.dto.OrderPaymentResponse;
import com.kartify.api.order.dto.PaymentAttemptResponse;
import com.kartify.api.order.enums.PaymentStatus;
import com.kartify.api.payment.dto.PaymentLink;
import com.kartify.api.payment.dto.PaymentLinkRequest;
import com.kartify.api.payment.service.PaymongoService;

@Service
public class CheckoutService {

  private static final Logger log = LoggerFactory.getLogger(CheckoutService.class);
  
  private final PaymongoService paymongoService;
  private final PaymentService paymentService;


  public CheckoutService(
    OrderService orderService,
    PaymongoService paymongoService,
    PaymentService paymentService
  ){
    this.paymongoService = paymongoService;
    this.paymentService = paymentService;
  }


  // Process and Pay the Order
  public CheckoutResponse placeOrder(Long userId,  OrderCreateRequest payload){

    OrderPaymentResponse order = paymentService.createOrderAndPayment(userId, payload);

    try {

      PaymentLink paymentProvider = paymongoService.process(new PaymentLinkRequest(
        order.idempotencyKey(),
        order.totalAmount(),
        "Payment for Order " + order.orderNumber(),
        "Order Number: " + order.orderNumber()
      ));
  
      paymentService.update(
        order.paymentId(), 
        order.orderId(), 
        paymentProvider.paymentLinkId(), 
        paymentProvider.checkoutUrl(), 
        paymentProvider.payload()
      );
  
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


  // Retry Payment
  public CheckoutResponse retryPayment(Long userId, Long orderId){

    PaymentAttemptResponse attemp = paymentService.prepareRetry(userId, orderId);

    try {

      PaymentLink paymentProvider = paymongoService.process(new PaymentLinkRequest(
        attemp.idempotencyKey(),
        attemp.totalAmount(),
        "Payment for Order " + attemp.orderNumber(),
        "Order Number: " + attemp.orderNumber()
      ));

      paymentService.completePaymentSetup(
        attemp.paymentId(), 
        paymentProvider.paymentLinkId(), 
        paymentProvider.checkoutUrl(), 
        paymentProvider.payload()
      );

      return new CheckoutResponse(
        paymentProvider.paymentLinkId(),
        paymentProvider.checkoutUrl()
      );

    } catch (PaymentCreationRejectedException e) {
      paymentService.updateStatus(attemp.paymentId(), attemp.orderId(), PaymentStatus.SETUP_FAILED);
      throw e;
    } catch (PaymentCreationUnknownException e) {
      paymentService.updateStatus(attemp.paymentId(), attemp.orderId(), PaymentStatus.CREATION_UNKNOWN);
      throw e;
    }

  }

}
