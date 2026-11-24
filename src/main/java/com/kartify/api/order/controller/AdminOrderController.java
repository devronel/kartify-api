package com.kartify.api.order.controller;

import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kartify.api.order.dto.AdminOrderDetailsResponse;
import com.kartify.api.order.dto.AdminOrderFilterRequest;
import com.kartify.api.order.dto.AdminOrderResponse;
import com.kartify.api.order.dto.AdminOrderStatusUpdateRequest;
import com.kartify.api.order.service.AdminOrderService;
import com.kartify.api.order.service.OrderCancellationService;
import com.kartify.api.security.CustomUserDetails;
import com.kartify.api.shared.dto.PaginationResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

  private final int DEFAULT_PAGE_SIZE = 10;
  private final int MAX_PAGE_SIZE = 30;

  private final AdminOrderService adminOrderService;
  private final OrderCancellationService orderCancellationService;

  public AdminOrderController(
    AdminOrderService adminOrderService,
    OrderCancellationService orderCancellationService
  ){
    this.adminOrderService = adminOrderService;
    this.orderCancellationService = orderCancellationService;
  }

  @GetMapping
  public ResponseEntity<PaginationResponse<AdminOrderResponse>> getAll(
    @ModelAttribute AdminOrderFilterRequest filterRequest,
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int pageSize
  ){

    if(pageSize < 1 || pageSize > MAX_PAGE_SIZE){
      throw new IllegalArgumentException("pageSize must be between 1 and " + MAX_PAGE_SIZE);
    }

    Pageable pageable = PageRequest.of(page-1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

    PaginationResponse<AdminOrderResponse> orders = adminOrderService.getAll(filterRequest, pageable);

    return ResponseEntity.ok(orders);

  }


  @GetMapping("/{orderNumber}")
  public ResponseEntity<AdminOrderDetailsResponse> getDetails(@PathVariable("orderNumber") String orderNumber){
    AdminOrderDetailsResponse order = adminOrderService.getDetails(orderNumber) ;
    return ResponseEntity.ok(order);
  }


  @PatchMapping("/{orderNumber}/status")
  public ResponseEntity<Void> updateStatus(
    @AuthenticationPrincipal CustomUserDetails principal,
    @PathVariable("orderNumber") String orderNumber,
    @Valid @RequestBody AdminOrderStatusUpdateRequest request
  ){
    
    adminOrderService.updateStatus(principal.getId(), orderNumber, request);

    return ResponseEntity.noContent().build();

  }


  @PostMapping("/{orderNumber}/cancel")
  public ResponseEntity<Void> cancelOrder(
    @AuthenticationPrincipal CustomUserDetails principal,
    @PathVariable("orderNumber") String orderNumber,
    @RequestBody Map<String, String> body
  ){
    String reason = body.get("reason");
    orderCancellationService.cancelOrderByAdmin(principal.getId(), orderNumber, reason);
    return ResponseEntity.noContent().build();
  }

}
