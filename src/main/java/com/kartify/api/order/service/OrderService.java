package com.kartify.api.order.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kartify.api.cart.dto.ItemProblem;
import com.kartify.api.cart.entity.Cart;
import com.kartify.api.cart.entity.CartItem;
import com.kartify.api.cart.enums.ItemProblemReason;
import com.kartify.api.cart.repository.CartRepository;
import com.kartify.api.exception.CartItemsUnavailableException;
import com.kartify.api.exception.FieldValidationException;
import com.kartify.api.exception.ResourceNotFoundException;
import com.kartify.api.order.dto.OrderCreateRequest;
import com.kartify.api.order.dto.OrderCreateResponse;
import com.kartify.api.order.entity.Order;
import com.kartify.api.order.entity.OrderItem;
import com.kartify.api.order.enums.OrderStatus;
import com.kartify.api.order.repository.OrderRepository;
import com.kartify.api.product.entity.Product;
import com.kartify.api.product.entity.ProductVariant;
import com.kartify.api.product.repository.ProductRepository;
import com.kartify.api.product.repository.ProductVariantRepository;
import com.kartify.api.user.entity.UserAddress;
import com.kartify.api.user.repository.UserAddressRepository;

@Service
public class OrderService {

  private final CartRepository cartRepository;
  private final UserAddressRepository userAddressRepository;
  private final ProductRepository productRepository;
  private final ProductVariantRepository productVariantRepository;
  private final OrderRepository orderRepository;

  // Constructor
  public OrderService(
    CartRepository cartRepository,
    UserAddressRepository userAddressRepository,
    ProductRepository productRepository,
    ProductVariantRepository productVariantRepository,
    OrderRepository orderRepository
  ){
    this.cartRepository = cartRepository;
    this.userAddressRepository = userAddressRepository;
    this.productRepository = productRepository;
    this.productVariantRepository = productVariantRepository;
    this.orderRepository = orderRepository;
  }

  // Create order
  @Transactional
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
    order.setOrderNumber("ORD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
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
      }
      orderItem.setProductName(product.getName());
      orderItem.setSku(variant != null ? variant.getSku() : product.getSku());
      orderItem.setPrice(unitPrice);
      orderItem.setQuantity(item.getQuantity());
      orderItem.setLineTotal(unitPrice.multiply(BigDecimal.valueOf(item.getQuantity())));
      order.addOrderItem(orderItem);
      // TODO: variant label, product image
    }


    // 7. Add order status history
    order.addStatusHistory(OrderStatus.PENDING, null, address.getUser());


    // 8. Save new order
    Order orderCreated = orderRepository.save(order);


    // 9 Clear the cart
    cart.getItems().clear();
    
    
    // 10. Lock rows in the same order for every transaction
    decrementStock(cartItems);


    return new OrderCreateResponse(
      orderCreated.getId(),
      orderCreated.getTotal()
    );

  }


  public List<ItemProblem> findProblems(List<CartItem> items) {
    List<ItemProblem> problems = new ArrayList<>();
    for (CartItem item : items) {
        checkItem(item).ifPresent(problems::add);
    }
    return problems;
  }


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
