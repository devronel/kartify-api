package com.kartify.api.order.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kartify.api.cart.dto.ItemProblem;
import com.kartify.api.cart.entity.Cart;
import com.kartify.api.cart.entity.CartItem;
import com.kartify.api.cart.enums.ItemProblemReason;
import com.kartify.api.cart.repository.CartRepository;
import com.kartify.api.contract.FileStorage;
import com.kartify.api.exception.CartItemsUnavailableException;
import com.kartify.api.exception.FieldValidationException;
import com.kartify.api.exception.ResourceNotFoundException;
import com.kartify.api.order.dto.OrderCreateRequest;
import com.kartify.api.order.dto.OrderCreateResponse;
import com.kartify.api.order.dto.OrderDetailsResponse;
import com.kartify.api.order.dto.OrderItemsResponse;
import com.kartify.api.order.dto.OrderResponse;
import com.kartify.api.order.dto.OrderStatusHistoryResponse;
import com.kartify.api.order.dto.PaymentSummaryResponse;
import com.kartify.api.order.entity.Order;
import com.kartify.api.order.entity.OrderItem;
import com.kartify.api.order.entity.Payment;
import com.kartify.api.order.enums.OrderStatus;
import com.kartify.api.order.enums.PaymentAction;
import com.kartify.api.order.repository.OrderItemRepository;
import com.kartify.api.order.repository.OrderRepository;
import com.kartify.api.order.repository.PaymentRepository;
import com.kartify.api.product.entity.Product;
import com.kartify.api.product.entity.ProductVariant;
import com.kartify.api.product.repository.ProductRepository;
import com.kartify.api.product.repository.ProductVariantRepository;
import com.kartify.api.shared.dto.PaginationResponse;
import com.kartify.api.user.entity.User;
import com.kartify.api.user.entity.UserAddress;
import com.kartify.api.user.repository.UserAddressRepository;
import com.kartify.api.user.repository.UserRepository;

@Service
public class OrderService {

  private static final Logger log = LoggerFactory.getLogger(OrderService.class);
  public static final Duration PENDING_HOLD = Duration.ofMinutes(30);

  private final CartRepository cartRepository;
  private final UserAddressRepository userAddressRepository;
  private final ProductRepository productRepository;
  private final ProductVariantRepository productVariantRepository;
  private final OrderRepository orderRepository;
  private final OrderItemRepository orderItemRepository;
  private final PaymentRepository paymentRepository;
  private final UserRepository userRepository;
  private final FileStorage fileStorage;

  // Constructor
  public OrderService(
    CartRepository cartRepository,
    UserAddressRepository userAddressRepository,
    ProductRepository productRepository,
    ProductVariantRepository productVariantRepository,
    OrderRepository orderRepository,
    OrderItemRepository orderItemRepository,
    PaymentRepository paymentRepository,
    UserRepository userRepository,
    FileStorage fileStorage
  ){
    this.cartRepository = cartRepository;
    this.userAddressRepository = userAddressRepository;
    this.productRepository = productRepository;
    this.productVariantRepository = productVariantRepository;
    this.orderRepository = orderRepository;
    this.orderItemRepository = orderItemRepository;
    this.paymentRepository = paymentRepository;
    this.userRepository = userRepository;
    this.fileStorage = fileStorage;
  }

  
  /**
   * Create new order
   * 
   * Includes creation of order items, order status history, decrement the stock quantity of the actual product
   * and clear the cart items after order creation.
   * 
   * Lock the cart table to prevent multiple transaction to modifying same data
   * 
   * @param userId
   * @param payload contains only the addressId of the users
   * @return order created id, order number and the total amount
   * @throws ResourceNotFoundException if no address found
   * @throws FieldValidationException if cart and cart item is empty
   * @throws CartItemsUnavailableException if theres problem with the cart item like the 
   *  actual product price changed, actual stock quantity is unavailable and etc.
   *  
   */
  public OrderCreateResponse create(Long userId, OrderCreateRequest payload){

    UserAddress address = userAddressRepository.findByIdAndUserId(payload.addressId(), userId)
      .orElseThrow(() -> new ResourceNotFoundException("Address not found."));


    Cart cart = cartRepository.findByUserIdForUpdate(userId)
      .orElseThrow(() -> new FieldValidationException("cart", "Your cart is empty."));


    List<CartItem> cartItems = cart.getItems();
    if(cartItems.isEmpty()){
      throw new FieldValidationException("cart", "Your cart is empty.");
    }


    // Collect every problem in the cart items and throw them together
    List<ItemProblem> problems = findProblems(cartItems);
    if (!problems.isEmpty()) {
      log.warn("Checkout blocked for user {}: {}", userId, problems);
      throw new CartItemsUnavailableException(problems);
    }


    BigDecimal subtotal = cartItemSubtotal(cartItems);
    BigDecimal shippingFee = BigDecimal.ZERO;
    BigDecimal discountAmount = BigDecimal.ZERO;


    Order order = new Order();
    order.setUser(address.getUser());
    order.setOrderNumber("ORD-" + Year.now().getValue() + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
    order.setStatus(OrderStatus.PENDING);
    order.setSubtotal(subtotal);
    order.setShippingFee(shippingFee);
    order.setDiscountAmount(discountAmount);
    order.setTotal(subtotal.add(shippingFee).subtract(discountAmount));
    order.setShippingRecipientName(address.getRecipientName());
    order.setShippingPhone(address.getPhone());
    order.setShippingAddressLine1(address.getAddressLine1());
    order.setShippingAddressLine2(address.getAddressLine2());
    order.setShippingBarangay(address.getBarangay());
    order.setShippingCity(address.getCity());
    order.setShippingProvince(address.getProvince());
    order.setShippingRegion(address.getRegion());
    order.setShippingPostalCode(address.getPostalCode());
    order.setShippingCountry(address.getCountry());


    for (CartItem item : cartItems) {
      
      Product product = item.getProduct();
      ProductVariant variant = item.getProductVariant();
      BigDecimal unitPrice = item.getPrice();

      OrderItem orderItem = new OrderItem();
      orderItem.setProduct(product);
      if(variant != null){
        orderItem.setProductVariant(variant);
        orderItem.setVariantLabel(variant.getAttributeValueName());
      }
      orderItem.setProductName(product.getName());
      orderItem.setSku(variant != null ? variant.getSku() : product.getSku());
      orderItem.setPrice(unitPrice);
      orderItem.setQuantity(item.getQuantity());
      orderItem.setLineTotal(unitPrice.multiply(BigDecimal.valueOf(item.getQuantity())));
      orderItem.setProductImage(item.getProduct().getPrimaryImage());
      order.addOrderItem(orderItem);
    }


    // Add order status history
    order.addStatusHistory(OrderStatus.PENDING, null, address.getUser());

    Order orderCreated = orderRepository.save(order);

    // Decrement stock of the actual product
    decrementStock(cartItems);

    cart.getItems().clear();

    return new OrderCreateResponse(
      orderCreated.getId(),
      order.getOrderNumber(),
      orderCreated.getTotal()
    );

  }


  /**
   * Change order status
   * 
   * Includes creation of new order status history, Also checking of the next status
   * because status have order like you can't transition from PAID to PENDING 
   * 
   * @param order
   * @param next order status
   * @param notes
   * @param changeBy entity who update the status
   * @throws IllegalStateException if the next status is wrong and invalid
   */
  @Transactional
  public void changeStatus(Order order, OrderStatus next, String notes, User changeBy){
    
    if(!order.getStatus().canTransitionTo(next)){
      throw new IllegalStateException(
        "Cannot change order " + order.getOrderNumber() + " from " + order.getStatus() + " to " + next
      );
    }

    order.setStatus(next);
    order.addStatusHistory(next, notes, changeBy);

  }


  /**
   * Get all user orders
   * 
   * Include the checking of each order if payable. List of items, items count
   * and associated primary images of items.
   * 
   * @param userId
   * @param status
   * @param pageable
   * @return pageable order response with list of order details
   */
  public PaginationResponse<OrderResponse> getAll(Long userId, OrderStatus status, Pageable pageable){

    Page<Order> orderPage = orderRepository.findByUserId(userId, status, pageable);

    List<Order> orders = orderPage.getContent();

    List<Long> orderIds = orders.stream().map(order -> order.getId()).toList();

    List<OrderItem> orderItems = orderItemRepository.findAllByOrderIds(orderIds);

    List<Payment> payments = paymentRepository.findAllDesc(orderIds);

    // Map for product items count per order item
    Map<Long, Integer> itemCountByOrderId = orderItems.stream()
      .collect(Collectors.groupingBy(
        orderItem -> orderItem.getOrder().getId(),
        Collectors.summingInt(item -> 1)
      ));


    // Map for list of product items per order item
    Map<Long, List<OrderItemsResponse>> orderItemsMap = orderItems.stream()
    .collect(Collectors.groupingBy(
      item -> item.getOrder().getId(),
      Collectors.mapping(orderItem -> new OrderItemsResponse(
        orderItem.getId(),
        orderItem.getProductName(),
        orderItem.getVariantLabel(),
        fileStorage.getUrl("files/public/product/images/" + orderItem.getProductImage()),
        orderItem.getPrice(),
        orderItem.getQuantity(),
        orderItem.getLineTotal()
      ), Collectors.toList())
    ));


    // Map of latest payment per order item
    Map<Long, PaymentSummaryResponse> latestPayments = payments.stream()
      .collect(Collectors.toMap(
        orderItem -> orderItem.getOrder().getId(),
        payment -> new PaymentSummaryResponse(
          payment.getStatus(),
          payment.getPaymentMethod(),
          payment.getCheckoutUrl(),
          payment.getPaidAt()
        ),
        (first, second) -> first
      ));


    LocalDateTime now = LocalDateTime.now();


    List<OrderResponse> orderResponse = orders.stream()
      .map(order -> {
        boolean isPayable = order.isPayable(now, PENDING_HOLD);
        boolean isExpired = order.isExpired(now, PENDING_HOLD);
        LocalDateTime expiresAt = order.getStatus() == OrderStatus.PENDING ? order.expiresAt(PENDING_HOLD) : null;
        PaymentSummaryResponse payment = latestPayments.getOrDefault(order.getId(), null);
        
        PaymentAction paymentAction = !isPayable ? PaymentAction.NONE : payment.status().paymentAction();

        return new OrderResponse(
          order.getOrderNumber(),
          order.getStatus(),
          payment.status(),
          payment.checkoutUrl(),
          order.getTotal(),
          itemCountByOrderId.getOrDefault(order.getId(), 0),
          paymentAction,
          isExpired,
          expiresAt,
          order.getCreatedAt(),
          orderItemsMap.get(order.getId()).stream().limit(3).toList()
        );

      }).toList();

    return new PaginationResponse<>(
      orderResponse,
      orderPage.getNumber() + 1,
      orderPage.getSize(),
      orderPage.getTotalElements(),
      orderPage.getTotalPages(),
      orderPage.hasNext(),
      orderPage.hasPrevious()
    );
  
  }


  /**
   * Get order details by urder id and order number
   * 
   * Includes all the information of orders such as product items, payment status,
   * order status histories and if the order is still payable or already expired.
   * 
   * @param userId
   * @param orderNumber
   * @return order details
   * @throws ResourceNotFoundException if order and payment not found.
   */
  public OrderDetailsResponse getDetail(Long userId, String orderNumber){
    
    Order order = orderRepository.findByOrderNumberAndUserId(orderNumber, userId)
      .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

    
    Payment payment = paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(order.getId())
      .orElseThrow(() -> new ResourceNotFoundException("Payment not Found."));    

    
    List<OrderItemsResponse> orderItems = order.getOrderItems().stream()
      .map(item -> new OrderItemsResponse(
        item.getId(),
        item.getProductName(),
        item.getVariantLabel(),
        fileStorage.getUrl("files/public/product/images/" + item.getProductImage()),
        item.getPrice(),
        item.getQuantity(),
        item.getLineTotal()
      ))
      .toList();

    
    PaymentSummaryResponse paymentSummaryResponse = new PaymentSummaryResponse(
      payment.getStatus(),
      payment.getPaymentMethod(),
      payment.getCheckoutUrl(),
      payment.getPaidAt()
    );


    // Get the list of order status history
    List<OrderStatusHistoryResponse> timeline = order.getOrderStatusHistories()
      .stream()
      .map(statusHistory -> new OrderStatusHistoryResponse(
        statusHistory.getStatus(),
        statusHistory.getCreatedAt()
      ))
      .toList();


    LocalDateTime now = LocalDateTime.now();
    boolean isPayable = order.isPayable(now, PENDING_HOLD);
    boolean isExpired = order.isExpired(now, PENDING_HOLD);
    LocalDateTime expiresAt = order.getStatus() == OrderStatus.PENDING ? order.expiresAt(PENDING_HOLD) : null;
    PaymentAction paymentAction = !isPayable ? PaymentAction.NONE : paymentSummaryResponse.status().paymentAction();

    return new OrderDetailsResponse(
      order.getOrderNumber(),
      order.getStatus(),
      order.getSubtotal(),
      order.getShippingFee(),
      order.getDiscountAmount(),
      order.getTotal(),
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
      order.getNotes(),
      paymentAction,
      isExpired,
      expiresAt,
      order.getCreatedAt(),
      orderItems,
      paymentSummaryResponse,
      timeline
    );
  
  }


  /**
   * Find problems in the cart items
   * 
   * It check problem like the actual product price not match in the cart items possibly because
   * admin changes the price, stock quantity is not available possibly 
   * the actual product is out of stock and variant mismatch and etc.
   * 
   * View the list of condition in checkItem() function
   * 
   * @param items from cart
   * @return list of item problem
   */
  public List<ItemProblem> findProblems(List<CartItem> items) {
    List<ItemProblem> problems = new ArrayList<>();
    for (CartItem item : items) {
      checkItem(item).ifPresent(problems::add);
    }
    return problems;
  }


  /**
   * Check the item the cart
   * 
   * It check problem like the actual product price not match in the cart items possibly because
   * admin changes the price, stock quantity is not available possibly 
   * the actual product is out of stock and variant mismatch and etc.
   * 
   * @param item
   * @return cart item problem or empty if no problem
   */
  public Optional<ItemProblem> checkItem(CartItem item) {
    Product product = item.getProduct();
    ProductVariant variant = item.getProductVariant();
    String name = product.getName();

    // hasVariants must match whether the cart item has a variant
    // Possible that the product in the cart don't have variant the actual product updated to have variant
    if (product.getHasVariants() != (variant != null)) {
      return Optional.of(
        new ItemProblem(
          item.getId(), 
          name, 
          ItemProblemReason.VARIANT_MISMATCH, 
          null
        )
      );
    }

    int stock;
    BigDecimal currentPrice;

    if (variant != null) {
        
      if (!variant.getIsActive()) {
          return Optional.of(
            new ItemProblem(
              item.getId(), 
              name, 
              ItemProblemReason.PRODUCT_UNAVAILABLE, 
              0
            )
          );
        }

        stock = variant.getStockQuantity();

        currentPrice = variant.getPrice();

    } else {

      stock = product.getStockQuantity();

      currentPrice = product.getPrice();

    }

    if(stock <= 0) {
      return Optional.of(
        new ItemProblem(
          item.getId(), 
          name, 
          ItemProblemReason.OUT_OF_STOCK, 
          0
        )
      );
    }

    if (stock < item.getQuantity()) {
      return Optional.of(
        new ItemProblem(
          item.getId(), 
          name, 
          ItemProblemReason.INSUFFICIENT_STOCK, 
          stock
        )
      );
    }

    if (currentPrice.compareTo(item.getPrice()) != 0) {
      return Optional.of(
        new ItemProblem(
          item.getId(), 
          name, 
          ItemProblemReason.PRICE_CHANGED, 
          stock
        )
      );
    }

    return Optional.empty();
  }


  /**
   * Decrements the stock of products or product variants in the cart.
   *
   * Processes stock updates in a consistent order to reduce the risk
   * of database deadlocks during concurrent checkouts.
   *
   * @param items the cart items whose stock should be decremented
   * @throws CartItemsUnavailableException if any item has insufficient stock
   */
  private void decrementStock(List<CartItem> items) {

    // Sort stock updates by type and ID to ensure concurrent checkout transactions
    // acquire database row locks in a consistent order, reducing the risk of deadlocks.
    List<CartItem> sorted = items.stream()
      .sorted(Comparator
        .comparing((CartItem i) -> i.getProductVariant() != null)
        .thenComparing(i -> i.getProductVariant() != null
          ? i.getProductVariant().getId()
          : i.getProduct().getId()))
      .toList();

    for (CartItem item : sorted) {

      int updated = (item.getProductVariant() != null)
        ? productVariantRepository.decrementStock(item.getProductVariant().getId(), item.getQuantity())
        : productRepository.decrementStock(item.getProduct().getId(), item.getQuantity());

      if (updated == 0) {
        throw new CartItemsUnavailableException(
          List.of(
            new ItemProblem(
              item.getId(),
              item.getProduct().getName(),
              ItemProblemReason.INSUFFICIENT_STOCK,
              null
            )
          )
        );
      }
    }
  }


  /**
   * Increment the stock of products or product variants.
   *
   * Processes stock updates in a consistent order to reduce the risk
   * of database deadlocks during concurrent transaction.
   * 
   * @param items
   */
  public void incrementStock(List<OrderItem> items) {

    // Sort stock updates by type and ID to ensure concurrent checkout transactions
    // acquire database row locks in a consistent order, reducing the risk of deadlocks.
    List<OrderItem> sorted = items.stream()
      .sorted(Comparator
        .comparing((OrderItem i) -> i.getProductVariant() != null)
        .thenComparing(i -> i.getProductVariant() != null
          ? i.getProductVariant().getId()
          : i.getProduct().getId()))
      .toList();

    for (OrderItem item : sorted) {

      int updated = (item.getProductVariant() != null)
        ? productVariantRepository.incrementStock(item.getProductVariant().getId(), item.getQuantity())
        : productRepository.incrementStock(item.getProduct().getId(), item.getQuantity());

      if (updated == 0) {
        log.warn("Could not restore stock for order item {}: product or variant not found", item.getId());
      }
    }
  }


  
  private BigDecimal cartItemSubtotal(List<CartItem> cartItems){

    return cartItems.stream()
      .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
      .reduce(BigDecimal.ZERO, (accumulator, next) -> accumulator.add(next)); 

  }
}
