package com.kartify.api.order.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kartify.api.contract.FileStorage;
import com.kartify.api.exception.FieldValidationException;
import com.kartify.api.exception.ResourceNotFoundException;
import com.kartify.api.order.dto.AddressInformationResponse;
import com.kartify.api.order.dto.AdminOrderDetailsResponse;
import com.kartify.api.order.dto.AdminOrderFilterRequest;
import com.kartify.api.order.dto.AdminOrderItemResponse;
import com.kartify.api.order.dto.AdminOrderPaymentResponse;
import com.kartify.api.order.dto.AdminOrderResponse;
import com.kartify.api.order.dto.AdminOrderStatusUpdateRequest;
import com.kartify.api.order.dto.AdminOrderTimelineResponse;
import com.kartify.api.order.entity.Order;
import com.kartify.api.order.entity.OrderItem;
import com.kartify.api.order.entity.OrderStatusHistory;
import com.kartify.api.order.entity.Payment;
import com.kartify.api.order.enums.AdminOrderAction;
import com.kartify.api.order.enums.OrderStatus;
import com.kartify.api.order.enums.PaymentStatus;
import com.kartify.api.order.repository.OrderItemRepository;
import com.kartify.api.order.repository.OrderRepository;
import com.kartify.api.order.repository.OrderSpecifications;
import com.kartify.api.order.repository.OrderStatusHistoryRepository;
import com.kartify.api.order.repository.PaymentRepository;
import com.kartify.api.shared.dto.PaginationResponse;
import com.kartify.api.user.entity.User;
import com.kartify.api.user.enums.Role;
import com.kartify.api.user.repository.UserRepository;

@Service
public class AdminOrderService {

  public static final Duration PENDING_HOLD = Duration.ofMinutes(30);
  public static final Set<OrderStatus> ADMIN_SETTABLE = EnumSet.of(
    OrderStatus.PROCESSING,
    OrderStatus.SHIPPED,
    OrderStatus.DELIVERED
  );

  private final OrderRepository orderRepository;
  private final OrderItemRepository orderItemRepository;
  private final PaymentRepository paymentRepository;
  private final OrderStatusHistoryRepository orderStatusHistoryRepository;
  private final UserRepository userRepository;
  private final FileStorage fileStorage;
  private final OrderService orderService;

  public AdminOrderService(
    OrderRepository orderRepository,
    OrderItemRepository orderItemRepository,
    PaymentRepository paymentRepository,
    OrderStatusHistoryRepository orderStatusHistoryRepository,
    UserRepository userRepository,
    FileStorage fileStorage,
    OrderService orderService
  ){
    this.orderRepository = orderRepository;
    this.orderItemRepository = orderItemRepository;
    this.paymentRepository = paymentRepository;
    this.orderStatusHistoryRepository = orderStatusHistoryRepository;
    this.userRepository = userRepository;
    this.fileStorage = fileStorage;
    this.orderService = orderService;
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
  public PaginationResponse<AdminOrderResponse> getAll(AdminOrderFilterRequest filterRequest, Pageable pageable){

    // Query Filters
    Specification<Order> specification = Specification.allOf(
      OrderSpecifications.keyword(filterRequest.search()),
      OrderSpecifications.status(filterRequest.status()),
      OrderSpecifications.dateRange(filterRequest.dateFrom(), filterRequest.dateTo())
    );

    Page<Order> orderPage = orderRepository.findAll(specification, pageable);

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

        boolean isExpired = order.isExpired(LocalDateTime.now(), PENDING_HOLD);

        return new AdminOrderResponse(
          order.getOrderNumber(),
          order.getShippingRecipientName(),
          order.getStatus(),
          mappedLatestPayment.getOrDefault(order.getId(), null),
          order.getTotal(),
          mappedOrderItemsCount.getOrDefault(order.getId(), null),
          order.getShippingCity() + ", " + order.getShippingProvince(),
          order.getCreatedAt(),
          mappedlatestOrderStatusHistory.getOrDefault(order.getId(), null),
          isExpired
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


  /**
   * Get order details
   * 
   * Includes all the order information such as order items, timelines
   * payments, amounts and shipping address that important to admin user
   * 
   * @param orderNumber
   * @return all the order information
   */
  public AdminOrderDetailsResponse getDetails(String orderNumber){

    Order order = orderRepository.findByOrderNumber(orderNumber)
      .orElseThrow(() -> new ResourceNotFoundException("Order not found"));


    AddressInformationResponse address = new AddressInformationResponse(
      null,
      null,
      order.getShippingRecipientName(),
      order.getShippingPhone(),
      order.getShippingAddressLine1(),
      order.getShippingAddressLine2(),
      order.getShippingBarangay(),
      order.getShippingCity(),
      order.getShippingProvince(),
      order.getShippingRegion(),
      order.getShippingPostalCode(),
      order.getShippingCountry(),
      null
    );


    List<AdminOrderItemResponse> orderItems = orderItemRepository.findAllByOrderId(order.getId())
      .stream()
      .map(orderItem -> {
        return new AdminOrderItemResponse(
          orderItem.getId(),
          orderItem.getProductName(),
          orderItem.getVariantLabel(),
          orderItem.getSku(),
          fileStorage.getUrl("files/public/product/images/" + orderItem.getProductImage()),
          orderItem.getPrice(),
          orderItem.getQuantity(),
          orderItem.getLineTotal()
        );
      }).toList();


    List<AdminOrderPaymentResponse> payments = paymentRepository.findAllByOrderIdOrderByCreatedAtDesc(order.getId())
      .stream()
      .map(payment -> new AdminOrderPaymentResponse(
        payment.getStatus(),
        payment.getPaymentMethod(),
        payment.getProvider(),
        payment.getProviderPaymentId(),
        payment.getAmount(),
        payment.getPaidAt(),
        payment.getCreatedAt()
      )).toList();


    List<AdminOrderTimelineResponse> timelines = orderStatusHistoryRepository.findAllByOrderIdOrderByCreatedAtDesc(order.getId())
      .stream()
      .map(orderStatusHistory -> new AdminOrderTimelineResponse(
        orderStatusHistory.getStatus(),
        orderStatusHistory.getNotes(),
        orderStatusHistory.getChangedBy() != null 
          ? orderStatusHistory.getChangedBy().getRole() == Role.ADMIN 
            ? "Changed by Admin" 
            : orderStatusHistory.getChangedBy().getFullName() 
          : "System",
        orderStatusHistory.getCreatedAt()
      ))
      .toList();


    OrderStatusHistory latestStatusHistory = orderStatusHistoryRepository.findFirstByOrderIdOrderByCreatedAtDesc(order.getId())
      .orElseThrow(() -> new ResourceNotFoundException("Latest Payment not found."));


    boolean isExpired = order.isExpired(LocalDateTime.now(), PENDING_HOLD);
    LocalDateTime expiresAt = order.getStatus() == OrderStatus.PENDING ? order.expiresAt(PENDING_HOLD) : null;

      
    return new AdminOrderDetailsResponse(
      order.getOrderNumber(),
      order.getStatus(),
      order.getCreatedAt(),
      latestStatusHistory.getCreatedAt(),
      order.getNotes(),
      expiresAt,
      isExpired,
      order.getUser().getFullName(),
      order.getUser().getEmail(),
      order.getSubtotal(),
      order.getShippingFee(),
      order.getDiscountAmount(),
      order.getTotal(),
      address,
      orderItems,
      payments,
      timelines,
      order.getStatus().adminActions()
    );

  }


  /**
   * Update order status
   * 
   * It can update status Processing, Shipped and Delivered only.
   * It create new order status history that include notes and the 
   * admin who call this function.
   * 
   * @param userId to get the admin id
   * @param orderNumber of order
   * @param payload include the status and notes
   */
  @Transactional
  public void updateStatus(Long userId, String orderNumber, AdminOrderStatusUpdateRequest payload){

    String notes = (payload.notes() == null || payload.notes().isBlank()) ? null : payload.notes().trim();

    if (payload.status() == OrderStatus.PAID || payload.status() == OrderStatus.CANCELLED) {
      throw new FieldValidationException("status", "This status can't be set here.");
    }

    if (payload.status() == OrderStatus.SHIPPED && notes == null) {
      throw new FieldValidationException("notes", "Enter the courier and tracking number.");
    }

    // Lock the order data before execute the update to avoid multiple transaction in the same data at the same time
    Order order = orderRepository.findByOrderNumberForUpdate(orderNumber)
      .orElseThrow(() -> new ResourceNotFoundException("Order not found."));

    boolean allowed = order.getStatus().adminActions().stream()
      .anyMatch(action -> action.target() == payload.status() && action != AdminOrderAction.CANCEL);

    if(!allowed){
      throw new FieldValidationException("status", "This action isn't available for the order's current status. Please refresh and try again.");
    }

    User user = userRepository.getReferenceById(userId);
    orderService.changeStatus(order, payload.status(), payload.notes(), user);

  }

}
