package com.kartify.api.cart.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kartify.api.cart.dto.CartItemCreateRequest;
import com.kartify.api.cart.dto.CartItemResponse;
import com.kartify.api.cart.dto.CartResponse;
import com.kartify.api.cart.dto.ItemQuantityUpdateResponse;
import com.kartify.api.cart.service.CartService;
import com.kartify.api.security.CustomUserDetails;

import jakarta.validation.Valid;


/*
  GET    /api/cart                    → view current user's cart (with items, totals)
  POST   /api/cart/items               → add an item to cart
  PATCH  /api/cart/items/{id}          → update quantity of a specific item
  DELETE /api/cart/items/{id}          → remove a specific item from cart
*/

@RestController
@RequestMapping("/api/cart")
public class CartController {

  private final int DEFAULT_PAGE_SIZE = 20;
  private final int MAX_PAGE_SIZE = 30;

  private final CartService cartService;

  public CartController(CartService cartService){
    this.cartService = cartService;
  }


  // Add new item in the cart
  @PostMapping
  public ResponseEntity<CartItemResponse> addItem(@AuthenticationPrincipal CustomUserDetails principal, @Valid @RequestBody CartItemCreateRequest request) {

    CartItemResponse cartItem = cartService.addItem(principal.getId(), request);

    return ResponseEntity.status(HttpStatus.CREATED).body(cartItem);

  }


  // Get all cart items
  @GetMapping
  public ResponseEntity<CartResponse> getAllItem(
    @AuthenticationPrincipal CustomUserDetails principal
  ) {

    CartResponse cartItem = cartService.getAllItems(principal.getId());

    return ResponseEntity.ok(cartItem);

  }


  // Remove item from the cart
  @DeleteMapping("/items/{id}")
  public ResponseEntity<Void> removeItem(
    @AuthenticationPrincipal CustomUserDetails principal,
    @PathVariable Long id
  ) {

    cartService.removeItem(principal.getId(), id);

    return ResponseEntity.noContent().build();

  }


  // Update item quantity
  @PatchMapping("/items/{id}")
  public ResponseEntity<ItemQuantityUpdateResponse> updateQuantity(
    @AuthenticationPrincipal CustomUserDetails principal,
    @PathVariable Long id,
    @RequestBody Map<String, Integer> body
  ) {

    Integer quantity = body.get("quantity");

    ItemQuantityUpdateResponse updateQuantity = cartService.updateQuantity(principal.getId(), id, quantity);

    return ResponseEntity.ok(updateQuantity);

  }


  @GetMapping("/items/count")
  public ResponseEntity<Integer> getTotalCartQuantity(@AuthenticationPrincipal CustomUserDetails principal) {

    Integer itemCount = cartService.getTotalCartQuantity(principal.getId());

    return ResponseEntity.ok(itemCount);

  }

}

