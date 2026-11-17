package com.kartify.api.cart.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.kartify.api.cart.dto.CartItemCreateRequest;
import com.kartify.api.cart.dto.CartItemResponse;
import com.kartify.api.cart.dto.CartResponse;
import com.kartify.api.cart.dto.ItemQuantityUpdateResponse;
import com.kartify.api.cart.entity.Cart;
import com.kartify.api.cart.entity.CartItem;
import com.kartify.api.cart.enums.CartStatus;
import com.kartify.api.cart.repository.CartItemRepository;
import com.kartify.api.cart.repository.CartRepository;
import com.kartify.api.contract.FileStorage;
import com.kartify.api.exception.FieldValidationException;
import com.kartify.api.exception.InsufficientStockException;
import com.kartify.api.exception.ResourceNotFoundException;
import com.kartify.api.product.entity.Product;
import com.kartify.api.product.entity.ProductVariant;
import com.kartify.api.product.repository.ProductRepository;
import com.kartify.api.product.repository.ProductVariantRepository;
import com.kartify.api.user.entity.User;
import com.kartify.api.user.repository.UserRepository;

@Service
public class CartService {

  private static final Logger log = LoggerFactory.getLogger(CartService.class);

  private final CartRepository cartRepository;
  private final CartItemRepository cartItemRepository;
  private final ProductRepository productRepository;
  private final ProductVariantRepository productVariantRepository;
  private final UserRepository userRepository;
  private final FileStorage fileStorage;

  public CartService(
    CartRepository cartRepository,
    CartItemRepository cartItemRepository,
    ProductRepository productRepository,
    ProductVariantRepository productVariantRepository,
    UserRepository userRepository,
    FileStorage fileStorage
  ){
    this.cartRepository = cartRepository;
    this.cartItemRepository = cartItemRepository;
    this.productRepository = productRepository;
    this.productVariantRepository = productVariantRepository;
    this.userRepository = userRepository;
    this.fileStorage = fileStorage;
  }

  
  /**
   * Adds a product to the user's cart.
   *
   * Creates a cart if the user does not have one and reactivates
   * an abandoned cart when necessary. If the product already exists
   * in the cart, its quantity is increased.
   *
   * @param userId the ID of the user
   * @param payload the product and quantity to add to the cart
   * @return the added or updated cart item response
   * @throws ResourceNotFoundException if the user, product, or specified product variant does not exist
   * @throws FieldValidationException if a required product variant is not selected
   * @throws InsufficientStockException if the requested quantity exceeds available stock
   */
  public CartItemResponse addItem(Long userId, CartItemCreateRequest payload){

    User user = userRepository.findById(userId)
      .orElseThrow(() -> new ResourceNotFoundException("User not found"));


    // Check if product exists
    Product product = productRepository.findById(payload.productId())
      .orElseThrow(() -> new ResourceNotFoundException("Product not found."));
      

    if(Boolean.TRUE.equals(product.getHasVariants()) && payload.productVariantId() == null){
      throw new FieldValidationException("productVariantId", "Please select a product option.");
    }


    ProductVariant variant = null;


    // Check if have variant and if the variant exists with that product
    if(payload.productVariantId() != null){

      variant = productVariantRepository.findByIdAndProductId(payload.productVariantId(), payload.productId())
        .orElseThrow(() -> new ResourceNotFoundException("Product Variant not found"));

    }


    // Check if user is already have cart
    Cart cart = cartRepository.findByUserId(user.getId())
      .orElseGet(() -> {
        Cart newCart = new Cart();
        newCart.setUser(user);
        newCart.setStatus(CartStatus.ACTIVE);
        return cartRepository.save(newCart);
      });


    // If cart is abandoned re-activate it
    if(cart.getStatus() == CartStatus.ABANDONED){
      cart.setStatus(CartStatus.ACTIVE);
      cartRepository.save(cart);
    }


    // Check if the uplcoming data already in the cart items
    Optional<CartItem> cartItem = payload.productVariantId() != null
      ? cartItemRepository.findByCartIdAndProductIdAndProductVariantId(cart.getId(), payload.productId(), payload.productVariantId())
      : cartItemRepository.findByCartIdAndProductIdAndProductVariantIdIsNull(cart.getId(), payload.productId());


    // Determine available stock (variant-level if applicable, otherwise product-level)
    Integer availableStock = variant != null ? variant.getStockQuantity() : product.getStockQuantity();


    CartItem saved;

      
    if (cartItem.isPresent()) {
      CartItem existing = cartItem.get();
      int newQuantity = existing.getQuantity() + payload.quantity();

      if (newQuantity > availableStock) {
          throw new InsufficientStockException("Not enough stock available.");
      }

      existing.setQuantity(newQuantity);
      saved = cartItemRepository.save(existing);

    } else {
      if (payload.quantity() > availableStock) {
        throw new InsufficientStockException("Not enough stock available.");
      }

      CartItem newItem = new CartItem();
      newItem.setCart(cart);
      newItem.setProduct(product);
      newItem.setProductVariant(variant);
      newItem.setQuantity(payload.quantity());

      BigDecimal price = (variant != null) ? variant.getPrice() : product.getPrice();
      newItem.setPrice(price);

      saved = cartItemRepository.save(newItem);
    }

    return toResponse(saved);

  }

  
  /**
   * Get all cart items
   * 
   * @param userId
   * @return cart response
   * @throws ResourceNotFoundException if no user found
   */
  public CartResponse getAllItems(Long userId){

    User user = userRepository.findById(userId)
      .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    if(user.getCart() == null){
      return new CartResponse(List.of(), 0, BigDecimal.ZERO, true);
    }
    
    List<CartItemResponse> cartItems = cartItemRepository.findAllByCartIdOrderByCreatedAtDesc(user.getCart().getId())
      .stream()
      .map(item -> toResponse(item))
      .toList();

    // Sum all the items quantity in the cart items
    int totalQuantity = cartItems.stream().mapToInt(item -> item.quantity()).sum();
    
    // Sum all the items subtotal in the cart items
    BigDecimal subtotal = cartItems.stream()
      .map(item -> item.subtotal())
      .reduce(BigDecimal.ZERO, (current , next) -> current.add(next));

    Boolean hasUnavailableItems = cartItems.stream().anyMatch(item -> !item.isAvailable());

    return new CartResponse(
      cartItems,
      totalQuantity,
      subtotal,
      hasUnavailableItems
    );

  }


  /**
   * Delete item from the cart
   * 
   * @param userId
   * @param cartItemId
   * @throws ResourceNotFoundException if no cart and cart items not found
   */
  public void removeItem(Long userId, Long cartItemId){

    Cart cart = cartRepository.findByUserId(userId)
      .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

    CartItem cartItem = cartItemRepository.findByIdAndCartId(cartItemId, cart.getId())
      .orElseThrow(() -> new ResourceNotFoundException("item not found in the cart"));

    cartItemRepository.delete(cartItem);

  }


  /**
   * Update cart item quantity
   * 
   * Recalculate the subtotal when the items quantity changed
   * 
   * @param userId
   * @param cartItemId
   * @param quantity
   * @return item quantity update
   * @throws IllegalArgumentException if quantity is less than zero
   * @throws ResourceNotFoundException if cart and cart items not found
   * @throws InsufficientStockException if current product stock is low than the requested stock quantity
   */
  public ItemQuantityUpdateResponse updateQuantity(Long userId, Long cartItemId, Integer quantity){

    if (quantity <= 0) {
      throw new IllegalArgumentException("Quantity must be greater than zero.");
    }

    Cart cart = cartRepository.findByUserId(userId)
      .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

    CartItem cartItem = cartItemRepository.findByIdAndCartId(cartItemId, cart.getId())
      .orElseThrow(() -> new ResourceNotFoundException("item not found in the cart"));

    Integer stockQuantity = cartItem.getProductVariant() != null 
      ? cartItem.getProductVariant().getStockQuantity() 
      : cartItem.getProduct().getStockQuantity();

    if(quantity > stockQuantity){
      throw new InsufficientStockException("Not enough stock available.");
    }

    cartItem.setQuantity(quantity);

    CartItem updateQuantity = cartItemRepository.save(cartItem);

    Integer qty = updateQuantity.getQuantity();
    BigDecimal subtotal = updateQuantity.getPrice().multiply(BigDecimal.valueOf(qty));

    Integer stock = cartItem.getProductVariant() != null ? cartItem.getProductVariant().getStockQuantity() : cartItem.getProduct().getStockQuantity();
    Integer availableStock = stock;
    Boolean isAvailable = stock > 0 && qty <= stock;

    return new ItemQuantityUpdateResponse(
      updateQuantity.getId(),
      qty,
      subtotal,
      availableStock,
      isAvailable
    );

  }


  /**
   * Get the total cart item quantity
   * 
   * @param userId
   * @return integer total of items in the cart
   */
  public Integer getTotalCartQuantity(Long userId){
    
    User user = userRepository.findById(userId)
      .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    if(user.getCart() == null){
      return 0;
    }
    
    return cartItemRepository.getTotalQuantityByCartId(user.getCart().getId());

  }

  
  // Helper
  private CartItemResponse toResponse(CartItem cartItem){

    String filename = cartItem.getProduct().getFiles().stream()
      .filter(file -> Boolean.TRUE.equals(file.getIsPrimary()))
      .map(file -> file.getFilename())
      .findFirst()
      .orElse(null);

    Long productVariantId = cartItem.getProductVariant() != null ? cartItem.getProductVariant().getId() : null;
    String primaryImageUrl = fileStorage.getUrl("files/public/product/images/" + filename);
    Integer quantity = cartItem.getQuantity();
    BigDecimal price = cartItem.getPrice();
    BigDecimal subtotal = price.multiply(BigDecimal.valueOf(quantity));

    Integer stock = cartItem.getProductVariant() != null ? cartItem.getProductVariant().getStockQuantity() : cartItem.getProduct().getStockQuantity();
    Integer availableStock = stock;
    Boolean isAvailable = stock > 0 && quantity <= stock;

    return new CartItemResponse(
      cartItem.getId(),
      cartItem.getProduct().getId(),
      productVariantId,
      cartItem.getProduct().getName(),
      primaryImageUrl,
      quantity,
      price,
      subtotal,
      availableStock,
      isAvailable
    );
  }

}
