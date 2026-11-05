package com.kartify.api.order.dto;

public record AddressInformationResponse(
  Long id,
  String label,
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
  Boolean isDefault
) {}
