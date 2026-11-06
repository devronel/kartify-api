package com.kartify.api.order.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Year;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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

@Service
public class OrderService {

  public static final Duration PENDING_HOLD = Duration.ofMinutes(30);

  private final CartRepository cartRepository;
  private final UserAddressRepository userAddressRepository;
  private final ProductRepository productRepository;
  private final ProductVariantRepository productVariantRepository;
  private final OrderRepository orderRepository;
  private final OrderItemRepository orderItemRepository;
  private final PaymentRepository paymentRepository;
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
    FileStorage fileStorage
  ){
    this.cartRepository = cartRepository;
    this.userAddressRepository = userAddressRepository;
    this.productRepository = productRepository;
    this.productVariantRepository = productVariantRepository;
    this.orderRepository = orderRepository;
    this.orderItemRepository = orderItemRepository;
    this.paymentRepository = paymentRepository;
    this.fileStorage = fileStorage;
  }

  // Create order
  // OrderCreateResponse
  public OrderCreateResponse create(Long userId, OrderCreateRequest payload){

    // 1. Get user address
    UserAddress address = userAddressRepository.findByIdAndUserId(payload.addressId(), userId)
      .orElseThrow(() -> new ResourceNotFoundException("Address not found."));

    // 2. Get all items in the user cart
    Cart cart = cartRepository.findByUserIdForUpdate(userId)
      .orElseThrow(() -> new FieldValidationException("cart", "Your cart is empty."));

    
    // 3.Get all the items in the cart and throw field validation if empty
    List<CartItem> cartItems = cart.getItems();
    if(cartItems.isEmpty()){
      throw new FieldValidationException("cart", "Your cart is empty.");
    }


    // 4. Collect every problem in the cart items and throw them together
    List<ItemProblem> problems = findProblems(cartItems);
    if (!problems.isEmpty()) {
      throw new CartItemsUnavailableException(problems);
    }

    BigDecimal subtotal = cartItemSubtotal(cartItems);
    BigDecimal shippingFee = BigDecimal.ZERO;
    BigDecimal discountAmount = BigDecimal.ZERO;


    // 5. Create new order
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


    // 6. Create order items
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

    // 7. Add order status history
    order.addStatusHistory(OrderStatus.PENDING, null, address.getUser());


    // 8. Save new order
    Order orderCreated = orderRepository.save(order);


    // 9. Lock rows in the same order for every transaction
    decrementStock(cartItems);

    
    // 10. Clear the cart
    cart.getItems().clear();


    return new OrderCreateResponse(
      orderCreated.getId(),
      order.getOrderNumber(),
      orderCreated.getTotal()
    );

  }


  // Change order status
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


  // Get all user orders
  public PaginationResponse<OrderResponse> getAll(Long userId, OrderStatus status, Pageable pageable){

    Page<Order> orderPage = orderRepository.findByUserId(userId, status, pageable);

    List<Order> orders = orderPage.getContent();

    List<Long> orderIds = orders.stream().map(order -> order.getId()).toList();

    List<OrderItem> orderItems = orderItemRepository.findAllByOrderIds(orderIds);

    List<Payment> payments = paymentRepository.findAllDesc(orderIds);

    // Count all the order items
    Map<Long, Integer> itemCountByOrderId = orderItems.stream()
      .collect(Collectors.groupingBy(
        orderItem -> orderItem.getOrder().getId(),
        Collectors.summingInt(item -> 1)
      ));


    // Create a map for order items
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


    // Create a map for latest payment in each order
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

    List<OrderResponse> orderResponse = orders.stream()
      .map(order -> {

        boolean pending = order.getStatus() == OrderStatus.PENDING;

        return new OrderResponse(
          order.getOrderNumber(),
          order.getStatus(),
          latestPayments.getOrDefault(order.getId(), null).status(),
          order.getTotal(),
          itemCountByOrderId.getOrDefault(order.getId(), 0),
          pending,
          pending ? order.getCreatedAt().plus(PENDING_HOLD) : null,
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


  // Get order details
  public OrderDetailsResponse getDetail(Long userId, String orderNumber){
    
    Order order = orderRepository.findByOrderNumberAndUserId(orderNumber, userId)
      .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

    Payment payment = paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(order.getId())
      .orElseThrow(() -> new ResourceNotFoundException("Payment not Found."));    

    
    // Get the list of order items
    List<OrderItemsResponse> orderItems = order.getOrderItems().stream()
      .map(item -> new OrderItemsResponse(
        item.getId(),
        item.getProductName(),
        item.getVariantLabel(),
        item.getProductImage(),
        item.getPrice(),
        item.getQuantity(),
        item.getLineTotal()
      ))
      .toList();

    
    // Get the latest payment summary
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

    boolean pending = order.getStatus() == OrderStatus.PAID;

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
      pending,
      pending ? order.getCreatedAt().plus(PENDING_HOLD) : null,
      orderItems,
      paymentSummaryResponse,
      timeline
    );
  
  }


  // Return all the errors of cart items
  public List<ItemProblem> findProblems(List<CartItem> items) {
    List<ItemProblem> problems = new ArrayList<>();
    for (CartItem item : items) {
      checkItem(item).ifPresent(problems::add);
    }
    return problems;
  }


  // Check cart item and return the error found
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


  // Decrement the product stock
  private void decrementStock(List<CartItem> items) {
    // Lock rows in the same order for every transaction: simple products first,
    // then variants, each by the ID of the row being updated
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


  // Sum all items in the cart
  private BigDecimal cartItemSubtotal(List<CartItem> cartItems){

    return cartItems.stream()
      .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
      .reduce(BigDecimal.ZERO, (accumulator, next) -> accumulator.add(next)); 

  }
}
