package com.kartify.api.order.controller;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kartify.api.order.dto.OrderResponse;
import com.kartify.api.order.service.OrderService;
import com.kartify.api.security.CustomUserDetails;
import com.kartify.api.shared.dto.PaginationResponse;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

  private final int DEFAULT_PAGE_SIZE = 10;
  private final int MAX_PAGE_SIZE = 30;

  private final OrderService orderService;

  public OrderController(OrderService orderService){
    this.orderService = orderService;
  }

  // Get all the order by user
  @GetMapping
  public ResponseEntity<PaginationResponse<OrderResponse>> getAllOrder(
    @AuthenticationPrincipal CustomUserDetails principal,
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int pageSize
  ){

    if(pageSize < 1 || pageSize > MAX_PAGE_SIZE){
      throw new IllegalArgumentException("pageSize must be between 1 and " + MAX_PAGE_SIZE);
    }

    Pageable pageable = PageRequest.of(page-1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

    PaginationResponse<OrderResponse> order = orderService.getAllOrder(principal.getId(), pageable);

    return ResponseEntity.ok(order);

  }

}
