package com.kartify.api.account.dto;

import com.kartify.api.user.enums.UserAddressType;

public record AddressResponse(
    String label,
    UserAddressType type,
    String recipientName,
    String phone,
    String addressLine1,
    String addressLine2,
    String barangay,
    String city,
    String province,
    String region,
    String postalCode,
    String country,
    Boolean isDefault
) {}
