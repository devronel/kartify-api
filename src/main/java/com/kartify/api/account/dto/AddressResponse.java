package com.kartify.api.account.dto;

import com.kartify.api.user.enums.UserAddressType;

public record AddressResponse(
    UserAddressType type
) {}
