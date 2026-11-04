package com.kartify.api.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.kartify.api.order.enums.OrderStatus;

public record OrderDetailsResponse(
  String orderNumber,
  OrderStatus status,

  BigDecimal subtotal,
  BigDecimal shippingFee,
  BigDecimal discountAmount,
  BigDecimal total,

  String shippingRecipientName,
  String shippingPhone,
  String shippingAddressLine1,
  String shippingAddressLine2,
  String shippingBarangay,
  String shippingCity,
  String shippingProvince,
  String shippingRegion,
  String shippingPostalCode,
  String shippingCountry,
  String notes,
  Boolean canPay,
  LocalDateTime expiresAt,

  List<OrderItemsResponse> orderItems,

  PaymentSummaryResponse payment,

  List<OrderStatusHistoryResponse> timeline
) {}
