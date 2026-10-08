package com.kartify.api.cart.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kartify.api.cart.dto.CartItemCreateRequest;
import com.kartify.api.cart.dto.CartItemResponse;
import com.kartify.api.cart.service.CartService;
import com.kartify.api.security.CustomUserDetails;
import com.kartify.api.shared.dto.PaginationResponse;

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
  public ResponseEntity<PaginationResponse<CartItemResponse>> getAllItem(
    @AuthenticationPrincipal CustomUserDetails principal,
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int pageSize
  ) {

    if(pageSize < 1 || pageSize > MAX_PAGE_SIZE){
        throw new IllegalArgumentException(
            "pageSize must be between 1 and " + MAX_PAGE_SIZE
        );
    }

    Pageable pageable = PageRequest.of(page-1, pageSize, Sort.by(Sort.Direction.DESC, "updatedAt"));

    PaginationResponse<CartItemResponse> cartItem = cartService.getAllItems(principal.getId(), pageable);

    return ResponseEntity.ok(cartItem);

  }

}

