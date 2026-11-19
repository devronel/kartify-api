package com.kartify.api.order.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.kartify.api.order.dto.AdminOrderResponse;
import com.kartify.api.order.entity.Order;
import com.kartify.api.order.entity.OrderItem;
import com.kartify.api.order.entity.OrderStatusHistory;
import com.kartify.api.order.entity.Payment;
import com.kartify.api.order.enums.PaymentStatus;
import com.kartify.api.order.repository.OrderItemRepository;
import com.kartify.api.order.repository.OrderRepository;
import com.kartify.api.order.repository.OrderStatusHistoryRepository;
import com.kartify.api.order.repository.PaymentRepository;
import com.kartify.api.shared.dto.PaginationResponse;

@Service
public class AdminOrderService {

  private final OrderRepository orderRepository;
  private final OrderItemRepository orderItemRepository;
  private final PaymentRepository paymentRepository;
  private final OrderStatusHistoryRepository orderStatusHistoryRepository;

  public AdminOrderService(
    OrderRepository orderRepository,
    OrderItemRepository orderItemRepository,
    PaymentRepository paymentRepository,
    OrderStatusHistoryRepository orderStatusHistoryRepository
  ){
    this.orderRepository = orderRepository;
    this.orderItemRepository = orderItemRepository;
    this.paymentRepository = paymentRepository;
    this.orderStatusHistoryRepository = orderStatusHistoryRepository;
  }

  
  /**
   * Get all orders
   * 
   * Includes the latest payments status, latest order status history date and
   * order items count
   * 
   * @param pageable
   * @return paginated order response
   */
  public PaginationResponse<AdminOrderResponse> getAll(Pageable pageable){

    Page<Order> orderPage = orderRepository.findAll(pageable);

    List<Order> orders = orderPage.getContent();

    List<Long> orderIds = orders.stream().map(order -> order.getId()).toList();

    List<OrderItem> orderItems = orderItemRepository.findAllByOrderIds(orderIds);
    Map<Long, Integer> mappedOrderItemsCount = orderItems.stream()
      .collect(Collectors.groupingBy(
        item -> item.getOrder().getId(),
        Collectors.summingInt(item -> 1)
      ));


    List<Payment> latestPayments = paymentRepository.findAllLatestByOrderIds(orderIds);
    Map<Long, PaymentStatus> mappedLatestPayment = latestPayments.stream()
      .collect(Collectors.toMap(
        payment -> payment.getOrder().getId(), 
        payment -> payment.getStatus()
      ));


    List<OrderStatusHistory> latestOrderStatusHistory = orderStatusHistoryRepository.findAllLatestByOrderIds(orderIds);
    Map<Long, LocalDateTime> mappedlatestOrderStatusHistory = latestOrderStatusHistory.stream()
      .collect(Collectors.toMap(
        payment -> payment.getOrder().getId(), 
        payment -> payment.getCreatedAt()
      ));

    List<AdminOrderResponse> formattedOrders = orders.stream()
      .map(order -> {
        return new AdminOrderResponse(
          order.getOrderNumber(),
          order.getShippingRecipientName(),
          order.getStatus(),
          mappedLatestPayment.getOrDefault(order.getId(), null),
          order.getTotal(),
          mappedOrderItemsCount.getOrDefault(order.getId(), null),
          order.getShippingCity() + ", " + order.getShippingProvince(),
          order.getCreatedAt(),
          mappedlatestOrderStatusHistory.getOrDefault(order.getId(), null)
        );
      }).toList();

    return new PaginationResponse<>(
      formattedOrders,
      orderPage.getNumber() + 1,
      orderPage.getSize(),
      orderPage.getTotalElements(),
      orderPage.getTotalPages(),
      orderPage.hasNext(),
      orderPage.hasPrevious()
    );

  }

}
