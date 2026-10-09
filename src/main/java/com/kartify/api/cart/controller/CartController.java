package com.kartify.api.cart.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kartify.api.cart.dto.CartItemCreateRequest;
import com.kartify.api.cart.dto.CartItemResponse;
import com.kartify.api.cart.dto.CartResponse;
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


  @PostMapping
  public ResponseEntity<CartItemResponse> addItem(@AuthenticationPrincipal CustomUserDetails principal, @Valid @RequestBody CartItemCreateRequest request) {

    CartItemResponse cartItem = cartService.addItem(principal.getId(), request);

    return ResponseEntity.status(HttpStatus.CREATED).body(cartItem);

  }


  @GetMapping
  public ResponseEntity<CartResponse> getAllItem(
    @AuthenticationPrincipal CustomUserDetails principal
  ) {

    CartResponse cartItem = cartService.getAllItems(principal.getId());

    return ResponseEntity.ok(cartItem);

  }

}

