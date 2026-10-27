package com.kartify.api.order.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kartify.api.cart.dto.ItemProblem;
import com.kartify.api.cart.entity.Cart;
import com.kartify.api.cart.entity.CartItem;
import com.kartify.api.cart.enums.ItemProblemReason;
import com.kartify.api.cart.repository.CartRepository;
import com.kartify.api.exception.CartItemsUnavailableException;
import com.kartify.api.order.dto.OrderCreateRequest;
import com.kartify.api.order.dto.OrderCreateResponse;
import com.kartify.api.order.entity.Order;
import com.kartify.api.order.enums.OrderStatus;
import com.kartify.api.order.enums.PaymentMethod;
import com.kartify.api.order.repository.OrderRepository;
import com.kartify.api.product.entity.Product;
import com.kartify.api.product.entity.ProductVariant;
import com.kartify.api.product.repository.ProductRepository;
import com.kartify.api.product.repository.ProductVariantRepository;
import com.kartify.api.user.entity.User;
import com.kartify.api.user.entity.UserAddress;
import com.kartify.api.user.repository.UserAddressRepository;

@ExtendWith(MockitoExtension.class)
public class OrderServiceCreateSuccessfulTest {

  @Mock
  private UserAddressRepository userAddressRepository;

  @Mock
  private OrderRepository orderRepository;

  @Mock
  private CartRepository cartRepository;

  @Mock
  private ProductRepository productRepository;

  @Mock
  private ProductVariantRepository productVariantRepository;

  @InjectMocks
  private OrderService orderService;

  private final Long userId = 1L;

  private OrderCreateRequest createOrderRequest(){
    return new OrderCreateRequest(
      1L,
      PaymentMethod.QRPh
    );
  }


  // Create Order
  @Test
  void createOrder_ShouldReturnSuccess_IfOrderIsCreated(){

    OrderCreateRequest payload = createOrderRequest();

    UserAddress address = new UserAddress();

    Product product = new Product();
    product.setStockQuantity(5);
    product.setPrice(BigDecimal.valueOf(200));

    CartItem cartItem = new CartItem();
    cartItem.setProduct(product);
    cartItem.setQuantity(2);
    cartItem.setPrice(BigDecimal.valueOf(200));

    Cart cart = new Cart();
    cart.addItem(cartItem);

    Order orderCreated = new Order();
    orderCreated.setTotal(BigDecimal.valueOf(400));

    when(userAddressRepository.findByIdAndUserId(userId, payload.addressId()))
      .thenReturn(Optional.of(address));

    when(cartRepository.findByUserIdForUpdate(userId))
      .thenReturn(Optional.of(cart));

    when(orderRepository.save(any(Order.class)))
      .thenReturn(orderCreated);

    when(productRepository.decrementStock(cartItem.getProduct().getId(), cartItem.getQuantity()))
      .thenReturn(1);

    OrderCreateResponse order = orderService.create(userId, payload);

    Assertions.assertTrue(order.totalAmount().equals(BigDecimal.valueOf(400)));
    Assertions.assertTrue(cart.getItems().isEmpty());

  }


  // Change Order Status
  @Nested
  class ChangeStatusOrderTest{

    @Test
    void changeStatusOrder_ShouldUpdateStatus_ifNoError(){

      Order order = new Order();
      order.setStatus(OrderStatus.PENDING);
      OrderStatus next = OrderStatus.PAID;
      String notes = "Paid"; 
      User changeBy = null;

      orderService.changeStatus(order, next, notes, changeBy);

      Assertions.assertEquals(OrderStatus.PAID, order.getStatus());

    }

  }
}
