package com.kartify.api.cart.service;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.kartify.api.cart.dto.CartItemCreateRequest;
import com.kartify.api.cart.dto.CartItemResponse;
import com.kartify.api.cart.entity.Cart;
import com.kartify.api.cart.entity.CartItem;
import com.kartify.api.cart.enums.CartStatus;
import com.kartify.api.cart.repository.CartItemRepository;
import com.kartify.api.cart.repository.CartRepository;
import com.kartify.api.contract.FileStorage;
import com.kartify.api.exception.FieldValidationException;
import com.kartify.api.exception.ResourceNotFoundException;
import com.kartify.api.product.entity.Product;
import com.kartify.api.product.entity.ProductFile;
import com.kartify.api.product.entity.ProductVariant;
import com.kartify.api.product.repository.ProductRepository;
import com.kartify.api.product.repository.ProductVariantRepository;
import com.kartify.api.user.entity.User;
import com.kartify.api.user.repository.UserRepository;

@Service
public class CartService {

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

  // Create cart and cart items
  public CartItemResponse addItem(Long userId, CartItemCreateRequest payload){

    User user = userRepository.findById(userId)
      .orElseThrow(() -> new ResourceNotFoundException("User not found"));


    // Check if product exists
    Product product = productRepository.findById(payload.productId())
      .orElseThrow(() -> new ResourceNotFoundException("Product not found."));
      

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


    //Check if the uplcoming data already in the cart items
    Optional<CartItem> cartItem = payload.productVariantId() != null
      ? cartItemRepository.findByCartIdAndProductIdAndProductVariantId(cart.getId(), payload.productId(), payload.productVariantId())
      : cartItemRepository.findByCartIdAndProductIdAndProductVariantIdIsNull(cart.getId(), payload.productId());


      // Determine available stock (variant-level if applicable, otherwise product-level)
      Integer availableStock = (variant != null) ? variant.getStockQuantity() : product.getStockQuantity();

      CartItem saved;

      if (cartItem.isPresent()) {
        CartItem existing = cartItem.get();
        int newQuantity = existing.getQuantity() + payload.quantity();

        if (newQuantity > availableStock) {
            throw new FieldValidationException("quantity", "Not enough stock available.");
        }

        existing.setQuantity(newQuantity);
        saved = cartItemRepository.save(existing);

      } else {
        if (payload.quantity() > availableStock) {
            throw new FieldValidationException("quantity", "Not enough stock available.");
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

    // Creating Response
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

      return new CartItemResponse(
        cartItem.getId(),
        cartItem.getProduct().getId(),
        productVariantId,
        cartItem.getProduct().getName(),
        primaryImageUrl,
        quantity,
        price,
        subtotal
      );
    }

}
