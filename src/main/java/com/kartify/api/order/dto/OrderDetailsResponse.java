package com.kartify.api.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.kartify.api.order.enums.OrderStatus;
import com.kartify.api.order.enums.PaymentMethod;
import com.kartify.api.order.enums.PaymentStatus;

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

  List<OrderItemsResponse> orderItems,

  PaymentMethod paymentMethod,
  PaymentStatus paymentStatus,
  String checkoutUrl,
  Instant paidAt
) {}
