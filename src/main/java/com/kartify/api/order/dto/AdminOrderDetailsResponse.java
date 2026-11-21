package com.kartify.api.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.kartify.api.order.enums.OrderStatus;

public record AdminOrderDetailsResponse(
  String orderNumber,
  OrderStatus orderStatus,
  LocalDateTime createdAt,
  LocalDateTime statusUpdatedAt,
  String notes,
  LocalDateTime expiresAt,
  boolean isExpired,
  String customerName,
  String customerEmail,
  BigDecimal subtotal,
  BigDecimal shippingFee,
  BigDecimal discountAmount,
  BigDecimal total,
  AddressInformationResponse address,
  List<AdminOrderItemResponse> orderItems,
  List<AdminOrderPaymentResponse> paymentAttempts,
  List<AdminOrderTimelineResponse> timelines
) {}
