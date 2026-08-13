package com.kartify.api.account.dto;

import com.kartify.api.user.enums.UserAddressType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AddressRequest(
    @NotBlank(message = "Label is required.") 
    @Size(max = 100) 
    String label,

    @NotNull(message = "Type is required.") 
    UserAddressType type,

    @NotBlank(message = "Recipient name is required.") 
    @Size(max = 100) 
    String recipientName,

    @NotBlank(message = "Phone is required.") 
    @Size(max = 30) 
    String phone,

    @NotBlank(message = "Address line 1 is required.") 
    @Size(max = 255) 
    String addressLine1,

    @Size(max = 255) 
    String addressLine2,

    @NotBlank(message = "Barangay is required.") 
    @Size(max = 100) 
    String barangay,

    @NotBlank(message = "City is required.") 
    @Size(max = 100) 
    String city,

    @NotBlank(message = "Province is required.") 
    @Size(max = 100) 
    String province,

    @NotBlank(message = "Region is required.") 
    @Size(max = 100) 
    String region,

    @NotBlank(message = "Postal code is required.") 
    @Size(max = 10) 
    String postalCode,

    @NotBlank(message = "Country is required.") 
    @Size(max = 50) 
    String country,
  
    Boolean isDefault
) {}
