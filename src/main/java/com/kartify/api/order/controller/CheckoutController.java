package com.kartify.api.order.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kartify.api.order.dto.CheckoutResponse;
import com.kartify.api.order.dto.OrderCreateRequest;
import com.kartify.api.order.service.CheckoutService;
import com.kartify.api.security.CustomUserDetails;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {

  private final CheckoutService checkoutService;

  public CheckoutController(CheckoutService checkoutService){
    this.checkoutService = checkoutService;
  }


  // Create order
  @PostMapping
  public ResponseEntity<CheckoutResponse> create(
    @AuthenticationPrincipal CustomUserDetails principal,
    @RequestBody OrderCreateRequest request
  ){

    CheckoutResponse checkout = checkoutService.placeOrder(principal.getId(), request);

    return ResponseEntity.status(HttpStatus.CREATED).body(checkout);

  }

}
