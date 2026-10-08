package com.kartify.api.cart.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kartify.api.cart.dto.CartItemCreateRequest;
import com.kartify.api.cart.entity.Cart;
import com.kartify.api.cart.entity.CartItem;
import com.kartify.api.cart.enums.CartStatus;
import com.kartify.api.cart.repository.CartItemRepository;
import com.kartify.api.cart.repository.CartRepository;
import com.kartify.api.contract.FileStorage;
import com.kartify.api.exception.FieldValidationException;
import com.kartify.api.exception.ResourceNotFoundException;
import com.kartify.api.product.entity.Product;
import com.kartify.api.product.entity.ProductVariant;
import com.kartify.api.product.repository.ProductRepository;
import com.kartify.api.product.repository.ProductVariantRepository;
import com.kartify.api.user.entity.User;
import com.kartify.api.user.repository.UserRepository;

/*
  My rules for testing:
  - Arrange
    - Set up all the needed test data
    - Mock dependencies and define their expected behavior
    - Prepare the scenario that I want to test

  - Act
    - Call the actual method being tested
    - Do not put the method's logic here

  - Assert
    - Check the expected result
    - Check if the expected exception was thrown
    - Verify important interactions with mocked dependencies
*/

@ExtendWith(MockitoExtension.class)
public class CartServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private ProductRepository productRepository;

  @Mock
  private ProductVariantRepository productVariantRepository;

  @Mock 
  private CartRepository cartRepository;

  @Mock
  private CartItemRepository cartItemRepository;

  @Mock
  private FileStorage fileStorage;

  @InjectMocks
  CartService cartService;

  Long userId;
  Long productId;
  CartItemCreateRequest request;

  @BeforeEach
  void initializeDep(){
    userId = 1L;
    productId = 1L;
    request = new CartItemCreateRequest(1L, 2L, 3);
  }


  @Test
  @DisplayName("Should throw ResourceNotFoundException when user does not exist")
  void testAddItemIfUserIsNotExist(){

    when(userRepository.findById(userId))
      .thenReturn(Optional.empty());

    Assertions.assertThrows(ResourceNotFoundException.class, () -> {
      cartService.addItem(userId, request);
    });

  }

  @Test
  @DisplayName("Should throw ResourceNotFoundException when product does not exist")
  void testAddItemIfProductIsNotExist(){

    User user = new User();

    when(userRepository.findById(userId))
      .thenReturn(Optional.of(user));

    when(productRepository.findById(productId))
      .thenReturn(Optional.empty());

    Assertions.assertThrows(ResourceNotFoundException.class, () -> {
      cartService.addItem(userId, request);
    });

  }


  @Test
  @DisplayName("Should throw ResourceNotFoundException when product variant does not exist")
  void testAddItemIfProductVariantIsNotExist(){

    request = new CartItemCreateRequest(1L, 1L, 3);

    User user = new User();
    Product product = new Product();

    when(userRepository.findById(userId))
      .thenReturn(Optional.of(user));

    when(productRepository.findById(productId))
      .thenReturn(Optional.of(product));

    when(productVariantRepository.findByIdAndProductId(request.productVariantId(), request.productId()))
      .thenReturn(Optional.empty());

    Assertions.assertThrows(ResourceNotFoundException.class, () -> {
      cartService.addItem(userId, request);
    });

  }


  @Test
  @DisplayName("Should use existing cart when user already has a cart")
  void testAddItemIfUserAlreadyHasCart() {

    // Arrange
    request = new CartItemCreateRequest(1L, 1L, 3);

    User user = new User();

    Product product = new Product();
    product.setStockQuantity(10);

    ProductVariant variant = new ProductVariant();
    variant.setStockQuantity(10);
    variant.setPrice(BigDecimal.valueOf(100));

    Cart cart = new Cart();
    cart.setUser(user);
    cart.setStatus(CartStatus.ACTIVE);

    CartItem savedCartItem = new CartItem();
    savedCartItem.setCart(cart);
    savedCartItem.setProduct(product);
    savedCartItem.setProductVariant(variant);
    savedCartItem.setQuantity(request.quantity());
    savedCartItem.setPrice(variant.getPrice());

    when(userRepository.findById(userId))
      .thenReturn(Optional.of(user));

    when(productRepository.findById(request.productId()))
      .thenReturn(Optional.of(product));

    when(productVariantRepository.findByIdAndProductId(
      request.productVariantId(),
      request.productId()
    )).thenReturn(Optional.of(variant));

    when(cartRepository.findByUserId(user.getId()))
      .thenReturn(Optional.of(cart));

    when(cartItemRepository.findByCartIdAndProductIdAndProductVariantId(
      cart.getId(),
      request.productId(),
      request.productVariantId()
    )).thenReturn(Optional.empty());

    when(cartItemRepository.save(any(CartItem.class)))
      .thenReturn(savedCartItem);

    when(fileStorage.getUrl(anyString()))
      .thenReturn("http://localhost/test-image.jpg");

    // Act
    cartService.addItem(userId, request);

    // Assert
    verify(cartRepository, never()).save(any(Cart.class));
  }


  @Test
  @DisplayName("Should create new cart when user don't have cart")
  void testAddItemIfUserDontHaveCart() {

    // Arrange
    request = new CartItemCreateRequest(1L, 1L, 3);

    User user = new User();

    Product product = new Product();
    product.setStockQuantity(10);

    ProductVariant variant = new ProductVariant();
    variant.setStockQuantity(10);
    variant.setPrice(BigDecimal.valueOf(100));

    Cart cart = new Cart();
    cart.setUser(user);
    cart.setStatus(CartStatus.ACTIVE);

    CartItem savedCartItem = new CartItem();
    savedCartItem.setCart(cart);
    savedCartItem.setProduct(product);
    savedCartItem.setProductVariant(variant);
    savedCartItem.setQuantity(request.quantity());
    savedCartItem.setPrice(variant.getPrice());

    when(userRepository.findById(userId))
      .thenReturn(Optional.of(user));

    when(productRepository.findById(request.productId()))
      .thenReturn(Optional.of(product));

    when(productVariantRepository.findByIdAndProductId(
      request.productVariantId(),
      request.productId()
    )).thenReturn(Optional.of(variant));

    when(cartRepository.findByUserId(user.getId()))
      .thenReturn(Optional.empty());

    when(cartRepository.save(any(Cart.class)))
      .thenReturn(cart);

    when(cartItemRepository.findByCartIdAndProductIdAndProductVariantId(
      cart.getId(),
      request.productId(),
      request.productVariantId()
    )).thenReturn(Optional.empty());

    when(cartItemRepository.save(any(CartItem.class)))
      .thenReturn(savedCartItem);

    when(fileStorage.getUrl(anyString()))
      .thenReturn("http://localhost/test-image.jpg");

    // Act
    cartService.addItem(userId, request);

    // Assert
    verify(cartRepository).save(any(Cart.class));
  }


  @Test
  @DisplayName("Should make the cart status ACTIVE if it is ABANDONED")
  void testAddItemMakeTheCartStatusActive() {

    // Arrange
    request = new CartItemCreateRequest(1L, 1L, 3);

    User user = new User();

    Product product = new Product();
    product.setStockQuantity(10);

    ProductVariant variant = new ProductVariant();
    variant.setStockQuantity(10);
    variant.setPrice(BigDecimal.valueOf(100));

    Cart cart = new Cart();
    cart.setUser(user);
    cart.setStatus(CartStatus.ABANDONED);

    CartItem savedCartItem = new CartItem();
    savedCartItem.setCart(cart);
    savedCartItem.setProduct(product);
    savedCartItem.setProductVariant(variant);
    savedCartItem.setQuantity(request.quantity());
    savedCartItem.setPrice(variant.getPrice());

    when(userRepository.findById(userId))
      .thenReturn(Optional.of(user));

    when(productRepository.findById(request.productId()))
      .thenReturn(Optional.of(product));

    when(productVariantRepository.findByIdAndProductId(
      request.productVariantId(),
      request.productId()
    )).thenReturn(Optional.of(variant));

    when(cartRepository.findByUserId(user.getId()))
      .thenReturn(Optional.of(cart));

    when(cartItemRepository.findByCartIdAndProductIdAndProductVariantId(
      cart.getId(),
      request.productId(),
      request.productVariantId()
    )).thenReturn(Optional.empty());

    when(cartItemRepository.save(any(CartItem.class)))
      .thenReturn(savedCartItem);

    when(fileStorage.getUrl(anyString()))
      .thenReturn("http://localhost/test-image.jpg");

    // Act
    cartService.addItem(userId, request);

    // Assert
    verify(cartRepository).save(any(Cart.class));
    Assertions.assertEquals(CartStatus.ACTIVE, cart.getStatus());
  }

  @Test
  @DisplayName("Should throw FieldValidationException if the quantity is greater than from available stock")
  void testAddItemQuantityGreaterThanAvailableStock() {

    // Arrange
    request = new CartItemCreateRequest(1L, 1L, 3);

    User user = new User();

    Product product = new Product();
    product.setStockQuantity(0);

    ProductVariant variant = new ProductVariant();
    variant.setStockQuantity(0);
    variant.setPrice(BigDecimal.valueOf(100));

    Cart cart = new Cart();
    cart.setUser(user);
    cart.setStatus(CartStatus.ACTIVE);

    CartItem savedCartItem = new CartItem();
    savedCartItem.setCart(cart);
    savedCartItem.setProduct(product);
    savedCartItem.setProductVariant(variant);
    savedCartItem.setQuantity(request.quantity());
    savedCartItem.setPrice(variant.getPrice());

    when(userRepository.findById(userId))
      .thenReturn(Optional.of(user));

    when(productRepository.findById(request.productId()))
      .thenReturn(Optional.of(product));

    when(productVariantRepository.findByIdAndProductId(
      request.productVariantId(),
      request.productId()
    )).thenReturn(Optional.of(variant));

    when(cartRepository.findByUserId(user.getId()))
      .thenReturn(Optional.of(cart));

    when(cartItemRepository.findByCartIdAndProductIdAndProductVariantId(
      cart.getId(),
      request.productId(),
      request.productVariantId()
    )).thenReturn(Optional.empty());

    // Assert
    Assertions.assertThrows(FieldValidationException.class, () -> {
      cartService.addItem(userId, request);
    });
  }

}
