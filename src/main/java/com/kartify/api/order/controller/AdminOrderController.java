package com.kartify.api.order.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kartify.api.order.dto.AdminOrderDetailsResponse;
import com.kartify.api.order.dto.AdminOrderResponse;
import com.kartify.api.order.service.AdminOrderService;
import com.kartify.api.shared.dto.PaginationResponse;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

  private final int DEFAULT_PAGE_SIZE = 10;
  private final int MAX_PAGE_SIZE = 30;

  private final AdminOrderService adminOrderService;

  public AdminOrderController(AdminOrderService adminOrderService){
    this.adminOrderService = adminOrderService;
  }

  @GetMapping
  public ResponseEntity<PaginationResponse<AdminOrderResponse>> getAll(
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int pageSize
  ){

    if(pageSize < 1 || pageSize > MAX_PAGE_SIZE){
      throw new IllegalArgumentException("pageSize must be between 1 and " + MAX_PAGE_SIZE);
    }

    Pageable pageable = PageRequest.of(page-1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

    PaginationResponse<AdminOrderResponse> orders = adminOrderService.getAll(pageable);

    return ResponseEntity.ok(orders);

  }


  @GetMapping("/{orderNumber}")
  public ResponseEntity<AdminOrderDetailsResponse> getDetails(@PathVariable("orderNumber") String orderNumber){
    AdminOrderDetailsResponse order = adminOrderService.getDetails(orderNumber) ;
    return ResponseEntity.ok(order);
  }

}
