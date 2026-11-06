package com.kartify.api.order.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kartify.api.order.dto.OrderDetailsResponse;
import com.kartify.api.order.dto.OrderResponse;
import com.kartify.api.order.enums.OrderStatus;
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
  public ResponseEntity<PaginationResponse<OrderResponse>> getAll(
    @AuthenticationPrincipal CustomUserDetails principal,
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int pageSize,
    @RequestParam(required = false) OrderStatus status
  ){

    if(pageSize < 1 || pageSize > MAX_PAGE_SIZE){
      throw new IllegalArgumentException("pageSize must be between 1 and " + MAX_PAGE_SIZE);
    }

    Pageable pageable = PageRequest.of(page-1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

    PaginationResponse<OrderResponse> order = orderService.getAll(principal.getId(), status, pageable);

    return ResponseEntity.ok(order);

  }


  // Get order by id
  @GetMapping("/{orderNumber}")
  public ResponseEntity<OrderDetailsResponse> getDetail(
    @AuthenticationPrincipal CustomUserDetails principal,
    @PathVariable String orderNumber
  ){

    OrderDetailsResponse order = orderService.getDetail(principal.getId(), orderNumber);

    return ResponseEntity.ok(order);

  }

}
