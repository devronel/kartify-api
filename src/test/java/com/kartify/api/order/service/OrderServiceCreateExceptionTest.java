package com.kartify.api.order.service;

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
import com.kartify.api.exception.FieldValidationException;
import com.kartify.api.exception.ResourceNotFoundException;
import com.kartify.api.order.dto.OrderCreateRequest;
import com.kartify.api.order.enums.PaymentMethod;
import com.kartify.api.product.entity.Product;
import com.kartify.api.product.entity.ProductVariant;
import com.kartify.api.user.entity.UserAddress;
import com.kartify.api.user.repository.UserAddressRepository;

@ExtendWith(MockitoExtension.class)
public class OrderServiceCreateExceptionTest {

  @Mock
  private UserAddressRepository userAddressRepository;

  @Mock
  private CartRepository cartRepository;

  @InjectMocks
  private OrderService orderService;

  private final Long userId = 1L;

  private OrderCreateRequest createOrderRequest(){
    return new OrderCreateRequest(
      1L,
      PaymentMethod.QRPh
    );
  }

  @Nested
  class CreateOrderTest{

    @Test
    void createOrder_ShouldThrowException_IfAddressNotFound(){

      OrderCreateRequest payload = createOrderRequest();

      when(userAddressRepository.findByIdAndUserId(userId, payload.addressId()))
        .thenReturn(Optional.empty());

      Assertions.assertThrows(ResourceNotFoundException.class, () -> {
        orderService.create(userId, payload);
      });

    }


    @Test
    void createOrder_ShouldThrowException_IfCartNotFound(){

      OrderCreateRequest payload = createOrderRequest();

      UserAddress address = new UserAddress();

      when(userAddressRepository.findByIdAndUserId(userId, payload.addressId()))
        .thenReturn(Optional.of(address));

      when(cartRepository.findByUserIdForUpdate(userId))
        .thenReturn(Optional.empty());

      Assertions.assertThrows(FieldValidationException.class, () -> {
        orderService.create(userId, payload);
      });

    }

    @Test
    void createOrder_ShouldThrowException_IfCartDontHaveItem(){

      OrderCreateRequest payload = createOrderRequest();

      UserAddress address = new UserAddress();

      Cart cart = new Cart();

      when(userAddressRepository.findByIdAndUserId(userId, payload.addressId()))
        .thenReturn(Optional.of(address));

      when(cartRepository.findByUserIdForUpdate(userId))
        .thenReturn(Optional.of(cart));

      Assertions.assertThrows(FieldValidationException.class, () -> {
        orderService.create(userId, payload);
      });

    }


    @Test
    void createOrder_ShouldThrowException_IfCartItemsNotMatchToActualProduct(){

      OrderCreateRequest payload = createOrderRequest();

      UserAddress address = new UserAddress();

      Product product = new Product();
      product.setStockQuantity(2);
      product.setPrice(BigDecimal.valueOf(500));

      CartItem cartItem = new CartItem();
      cartItem.setProduct(product);
      cartItem.setQuantity(5);
      cartItem.setPrice(BigDecimal.valueOf(500));

      Cart cart = new Cart();
      cart.addItem(cartItem);

      when(userAddressRepository.findByIdAndUserId(userId, payload.addressId()))
        .thenReturn(Optional.of(address));

      when(cartRepository.findByUserIdForUpdate(userId))
        .thenReturn(Optional.of(cart));

      Assertions.assertThrows(CartItemsUnavailableException.class, () -> {
        orderService.create(userId, payload);
      });

    }


    @Test
    void createOrder_ShouldThrowException_IfCartItemQuantityIsLessThanProductQuantity(){

      OrderCreateRequest payload = createOrderRequest();

      UserAddress address = new UserAddress();

      Product product = new Product();
      product.setStockQuantity(2);
      product.setPrice(BigDecimal.valueOf(500));

      CartItem cartItem = new CartItem();
      cartItem.setProduct(product);
      cartItem.setQuantity(5);
      cartItem.setPrice(BigDecimal.valueOf(500));

      Cart cart = new Cart();
      cart.addItem(cartItem);

      when(userAddressRepository.findByIdAndUserId(userId, payload.addressId()))
        .thenReturn(Optional.of(address));

      when(cartRepository.findByUserIdForUpdate(userId))
        .thenReturn(Optional.of(cart));

      CartItemsUnavailableException exception = Assertions.assertThrows(CartItemsUnavailableException.class, () -> {
        orderService.create(userId, payload);
      });

      ItemProblem actualProblems = exception.getProblems().get(0);

      Assertions.assertNotNull(actualProblems);
      Assertions.assertEquals(ItemProblemReason.INSUFFICIENT_STOCK, actualProblems.reason());

    }


    // Test if product price is changed
    @Test
    void createOrder_ShouldThrowException_IfProductPriceChanged(){

      OrderCreateRequest payload = createOrderRequest();

      UserAddress address = new UserAddress();

      Product product = new Product();
      product.setStockQuantity(2);
      product.setPrice(BigDecimal.valueOf(500));

      CartItem cartItem = new CartItem();
      cartItem.setProduct(product);
      cartItem.setQuantity(2);
      cartItem.setPrice(BigDecimal.valueOf(200));

      Cart cart = new Cart();
      cart.addItem(cartItem);

      when(userAddressRepository.findByIdAndUserId(userId, payload.addressId()))
        .thenReturn(Optional.of(address));

      when(cartRepository.findByUserIdForUpdate(userId))
        .thenReturn(Optional.of(cart));

      CartItemsUnavailableException exception = Assertions.assertThrows(CartItemsUnavailableException.class, () -> {
        orderService.create(userId, payload);
      });

      ItemProblem actualProblem = exception.getProblems().get(0);

      Assertions.assertNotNull(actualProblem);
      Assertions.assertEquals(ItemProblemReason.PRICE_CHANGED, actualProblem.reason());

    }


    // Test if product variant price changed
    @Test
    void createOrder_ShouldThrowException_IfProductVariantPriceChanged(){

      OrderCreateRequest payload = createOrderRequest();

      UserAddress address = new UserAddress();

      ProductVariant variant = new ProductVariant();
      variant.setStockQuantity(2);
      variant.setPrice(BigDecimal.valueOf(500));

      Product product = new Product();
      product.addVariant(variant);
      product.setHasVariants(true);

      CartItem cartItem = new CartItem();
      cartItem.setProduct(product);
      cartItem.setProductVariant(variant);
      cartItem.setQuantity(2);
      cartItem.setPrice(BigDecimal.valueOf(200));

      Cart cart = new Cart();
      cart.addItem(cartItem);

      when(userAddressRepository.findByIdAndUserId(userId, payload.addressId()))
        .thenReturn(Optional.of(address));

      when(cartRepository.findByUserIdForUpdate(userId))
        .thenReturn(Optional.of(cart));

      CartItemsUnavailableException exception = Assertions.assertThrows(CartItemsUnavailableException.class, () -> {
        orderService.create(userId, payload);
      });

      ItemProblem actualProblem = exception.getProblems().get(0);

      Assertions.assertNotNull(actualProblem);
      Assertions.assertEquals(ItemProblemReason.PRICE_CHANGED, actualProblem.reason());

    }


    // If Product is out of stock
    @Test
    void createOrder_ShouldThrowException_IfProductIsOutOfStock(){

      OrderCreateRequest payload = createOrderRequest();

      UserAddress address = new UserAddress();

      Product product = new Product();
      product.setStockQuantity(0);
      product.setPrice(BigDecimal.valueOf(500));

      CartItem cartItem = new CartItem();
      cartItem.setProduct(product);
      cartItem.setQuantity(2);
      cartItem.setPrice(BigDecimal.valueOf(200));

      Cart cart = new Cart();
      cart.addItem(cartItem);

      when(userAddressRepository.findByIdAndUserId(userId, payload.addressId()))
        .thenReturn(Optional.of(address));

      when(cartRepository.findByUserIdForUpdate(userId))
        .thenReturn(Optional.of(cart));

      CartItemsUnavailableException exception = Assertions.assertThrows(CartItemsUnavailableException.class, () -> {
        orderService.create(userId, payload);
      });

      ItemProblem actualProblem = exception.getProblems().get(0);

      Assertions.assertNotNull(actualProblem);
      Assertions.assertEquals(ItemProblemReason.OUT_OF_STOCK, actualProblem.reason());

    }


    // If product variant is out of stock
    @Test
    void createOrder_ShouldThrowException_IfProductVariantIsOutOfStock(){

      OrderCreateRequest payload = createOrderRequest();

      UserAddress address = new UserAddress();

      ProductVariant variant = new ProductVariant();
      variant.setStockQuantity(0);
      variant.setPrice(BigDecimal.valueOf(500));
      variant.setIsActive(true);

      Product product = new Product();
      product.addVariant(variant);
      product.setHasVariants(true);

      CartItem cartItem = new CartItem();
      cartItem.setProduct(product);
      cartItem.setProductVariant(variant);
      cartItem.setQuantity(2);
      cartItem.setPrice(BigDecimal.valueOf(500));

      Cart cart = new Cart();
      cart.addItem(cartItem);

      when(userAddressRepository.findByIdAndUserId(userId, payload.addressId()))
        .thenReturn(Optional.of(address));

      when(cartRepository.findByUserIdForUpdate(userId))
        .thenReturn(Optional.of(cart));

      CartItemsUnavailableException exception = Assertions.assertThrows(CartItemsUnavailableException.class, () -> {
        orderService.create(userId, payload);
      });

      ItemProblem actualProblem = exception.getProblems().get(0);

      Assertions.assertNotNull(actualProblem);
      Assertions.assertEquals(ItemProblemReason.OUT_OF_STOCK, actualProblem.reason());

    }


    // If Product is insufficient of stock
    @Test
    void createOrder_ShouldThrowException_IfProductIsInsufficientStock(){

      OrderCreateRequest payload = createOrderRequest();

      UserAddress address = new UserAddress();

      Product product = new Product();
      product.setStockQuantity(2);
      product.setPrice(BigDecimal.valueOf(500));

      CartItem cartItem = new CartItem();
      cartItem.setProduct(product);
      cartItem.setQuantity(5);
      cartItem.setPrice(BigDecimal.valueOf(200));

      Cart cart = new Cart();
      cart.addItem(cartItem);

      when(userAddressRepository.findByIdAndUserId(userId, payload.addressId()))
        .thenReturn(Optional.of(address));

      when(cartRepository.findByUserIdForUpdate(userId))
        .thenReturn(Optional.of(cart));

      CartItemsUnavailableException exception = Assertions.assertThrows(CartItemsUnavailableException.class, () -> {
        orderService.create(userId, payload);
      });

      ItemProblem actualProblem = exception.getProblems().get(0);

      Assertions.assertNotNull(actualProblem);
      Assertions.assertEquals(ItemProblemReason.INSUFFICIENT_STOCK, actualProblem.reason());

    }


    // If product variant is insufficient stock
    @Test
    void createOrder_ShouldThrowException_IfProductVariantIsInsufficientStock(){

      OrderCreateRequest payload = createOrderRequest();

      UserAddress address = new UserAddress();

      ProductVariant variant = new ProductVariant();
      variant.setStockQuantity(2);
      variant.setPrice(BigDecimal.valueOf(500));
      variant.setIsActive(true);

      Product product = new Product();
      product.addVariant(variant);
      product.setHasVariants(true);

      CartItem cartItem = new CartItem();
      cartItem.setProduct(product);
      cartItem.setProductVariant(variant);
      cartItem.setQuantity(5);
      cartItem.setPrice(BigDecimal.valueOf(500));

      Cart cart = new Cart();
      cart.addItem(cartItem);

      when(userAddressRepository.findByIdAndUserId(userId, payload.addressId()))
        .thenReturn(Optional.of(address));

      when(cartRepository.findByUserIdForUpdate(userId))
        .thenReturn(Optional.of(cart));

      CartItemsUnavailableException exception = Assertions.assertThrows(CartItemsUnavailableException.class, () -> {
        orderService.create(userId, payload);
      });

      ItemProblem actualProblem = exception.getProblems().get(0);

      Assertions.assertNotNull(actualProblem);
      Assertions.assertEquals(ItemProblemReason.INSUFFICIENT_STOCK, actualProblem.reason());

    }


    // Test if the cart item don't have variant but the actual product updated to have variant it should throw an error and the message
    @Test
    void createOrder_ShouldThrowException_IfCartItemVariantMismatchToProduct(){

      OrderCreateRequest payload = createOrderRequest();

      UserAddress address = new UserAddress();

      ProductVariant variant1 = new ProductVariant();
      variant1.setStockQuantity(2);
      variant1.setPrice(BigDecimal.valueOf(1000));

      ProductVariant variant2 = new ProductVariant();
      variant2.setStockQuantity(2);
      variant2.setPrice(BigDecimal.valueOf(1000));

      Product product = new Product();
      product.addVariant(variant2);
      product.setHasVariants(true);
      product.setStockQuantity(0);
      product.setPrice(BigDecimal.valueOf(500));

      CartItem cartItem = new CartItem();
      cartItem.setProduct(product);
      cartItem.setQuantity(2);
      cartItem.setPrice(BigDecimal.valueOf(200));

      Cart cart = new Cart();
      cart.addItem(cartItem);

      when(userAddressRepository.findByIdAndUserId(userId, payload.addressId()))
        .thenReturn(Optional.of(address));

      when(cartRepository.findByUserIdForUpdate(userId))
        .thenReturn(Optional.of(cart));

      CartItemsUnavailableException exception = Assertions.assertThrows(CartItemsUnavailableException.class, () -> {
        orderService.create(userId, payload);
      });

      ItemProblem actualProblem = exception.getProblems().get(0);

      System.out.println(exception.getProblems());

      Assertions.assertNotNull(actualProblem);
      Assertions.assertEquals(ItemProblemReason.VARIANT_MISMATCH, actualProblem.reason());

    }


    @Test
    void createOrder_ShouldThrowException_IfCartItemVariantIsInActive(){

      OrderCreateRequest payload = createOrderRequest();

      UserAddress address = new UserAddress();

      ProductVariant variant1 = new ProductVariant();
      variant1.setStockQuantity(5);
      variant1.setPrice(BigDecimal.valueOf(200));
      variant1.setIsActive(false);

      Product product = new Product();
      product.addVariant(variant1);
      product.setHasVariants(true);

      CartItem cartItem = new CartItem();
      cartItem.setProduct(product);
      cartItem.setProductVariant(variant1);
      cartItem.setQuantity(2);
      cartItem.setPrice(BigDecimal.valueOf(200));

      Cart cart = new Cart();
      cart.addItem(cartItem);

      when(userAddressRepository.findByIdAndUserId(userId, payload.addressId()))
        .thenReturn(Optional.of(address));

      when(cartRepository.findByUserIdForUpdate(userId))
        .thenReturn(Optional.of(cart));

      CartItemsUnavailableException exception = Assertions.assertThrows(CartItemsUnavailableException.class, () -> {
        orderService.create(userId, payload);
      });

      ItemProblem actualProblem = exception.getProblems().get(0);

      System.out.println(exception.getProblems());

      Assertions.assertNotNull(actualProblem);
      Assertions.assertEquals(ItemProblemReason.PRODUCT_UNAVAILABLE, actualProblem.reason());

    }

  }

}
