package com.kartify.api.order.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kartify.api.exception.FieldValidationException;
import com.kartify.api.exception.ResourceNotFoundException;
import com.kartify.api.order.entity.Order;
import com.kartify.api.order.entity.Payment;
import com.kartify.api.order.enums.OrderStatus;
import com.kartify.api.order.enums.PaymentStatus;
import com.kartify.api.order.repository.OrderRepository;
import com.kartify.api.order.repository.PaymentRepository;
import com.kartify.api.user.entity.User;
import com.kartify.api.user.repository.UserRepository;

@Service
public class OrderCancellationService {

  private static final Logger log = LoggerFactory.getLogger(OrderCancellationService.class);
  private static final List<PaymentStatus> OPEN_PAYMENT_STATUSES = List.of(
    PaymentStatus.CREATING, 
    PaymentStatus.PENDING
  );

  private final OrderRepository orderRepository;
  private final PaymentRepository paymentRepository;
  private final UserRepository userRepository;
  private final OrderService orderService;

  // Constructor
  public OrderCancellationService(
    OrderRepository orderRepository,
    PaymentRepository paymentRepository,
    UserRepository userRepository,
    OrderService orderService
  ){
    this.orderRepository = orderRepository;
    this.paymentRepository = paymentRepository;
    this.userRepository = userRepository;
    this.orderService = orderService;
  }


  /**
   * Cancel the order of user
   * 
   * It cancell the order that have PENDING status only and lock the order
   * before transaction
   * 
   * Call the cancelLockOrder()
   * 
   * @param userId of customer
   * @param orderNumber
   * @param reason for cancelling the order
   */
  @Transactional
  public void cancelOrder(Long userId, String orderNumber, String reason){

    Order order = orderRepository.findByOrderNumberAndUseridForUpdate(orderNumber, userId)
      .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

    if(order.getStatus() != OrderStatus.PENDING){
      throw new FieldValidationException("status", "This order can't be cancel anymore.");
    }

    cancelLockOrder(order, reason, userRepository.getReferenceById(userId));

  }


  /**
   * Cancel Order By Admin user
   * 
   * Admin can cancel all the order doesn't matter what is the status
   * 
   * @param userId
   * @param orderNumber
   * @param reason
   */
  @Transactional
  public void cancelOrderByAdmin(Long userId, String orderNumber, String reason){

    Order order = orderRepository.findByOrderNumberForUpdate(orderNumber)
      .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

    cancelLockOrder(order, reason, userRepository.getReferenceById(userId));

  }


  /**
   * Change status of Order to CANCELLED
   * 
   * It include the release of stock back to the associated product and
   * update the all the open payment status into EXPIRED
   * 
   * @param order
   * @param reason
   * @param cancelledBy
   */
  public void cancelLockOrder(Order order, String reason, User cancelledBy){

    orderService.changeStatus(order, OrderStatus.CANCELLED, reason, cancelledBy);

    orderService.incrementStock(order.getOrderItems());

    List<Payment> payments = paymentRepository.findAllByOrderIdAndStatusIn(order.getId(), OPEN_PAYMENT_STATUSES);

    List<String> paymentProviderIds = new ArrayList<>();
    for (Payment payment : payments) {
      payment.setStatus(PaymentStatus.EXPIRED);
      Payment updatedPayment = paymentRepository.save(payment);
      if(updatedPayment.getProviderPaymentId() != null){
        paymentProviderIds.add(updatedPayment.getProviderPaymentId());
      }
    }


    // Archive the links after commit, This happen in the queue in inside of this transaction
    if(!paymentProviderIds.isEmpty()){
      // TODO:Dispatch the event here...
    }
    
  }

}
