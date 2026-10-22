package com.kartify.api.order;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

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

  // Constructor
  public OrderService(
    CartRepository cartRepository,
    UserAddressRepository userAddressRepository,
    ProductRepository productRepository,
    ProductVariantRepository productVariantRepository
  ){
    this.cartRepository = cartRepository;
    this.userAddressRepository = userAddressRepository;
    this.productRepository = productRepository;
    this.productVariantRepository = productVariantRepository;
  }

  // Create order
  @Transactional
  public void create(Long userId, OrderCreateRequest payload){

    // 1. Get all items in the user cart
    Cart cart = cartRepository.findByUserId(userId)
      .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

    
    // Get all the items in the cart and throw field validation if empty
    List<CartItem> cartItems = cart.getItems();
    if(cartItems.isEmpty()){
      throw new FieldValidationException("cart", "Your cart is empty.");
    }


    // Collect every problem in the cart items and throw them together
    List<ItemProblem> problems = findProblems(cartItems);
    if (!problems.isEmpty()) {
      throw new CartItemsUnavailableException(problems);
    }


    // Get user address
    UserAddress address = userAddressRepository.findByIdAndUserId(payload.addressId(), userId)
      .orElseThrow(() -> new ResourceNotFoundException("Address not found."));

    
    // Lock rows in the same order for every transaction
    decrementStock(cartItems);

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
}
