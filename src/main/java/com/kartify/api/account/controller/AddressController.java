package com.kartify.api.account.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kartify.api.account.dto.AddressRequest;
import com.kartify.api.account.dto.AddressResponse;
import com.kartify.api.account.dto.AddressUpdateRequest;
import com.kartify.api.account.service.AddressService;
import com.kartify.api.security.CustomUserDetails;
import com.kartify.api.shared.dto.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/account/address")
public class AddressController {

    protected final AddressService addressService;

    public AddressController(AddressService addressService){
        this.addressService = addressService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAddresses(@AuthenticationPrincipal CustomUserDetails principal){
        List<AddressResponse> userAddresses = addressService.getAddresses(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Address List", userAddresses));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AddressResponse>> getAddress(
        @AuthenticationPrincipal CustomUserDetails principal,
        @PathVariable Long id
    ){
        AddressResponse userAddress = addressService.getAddress(principal.getId(), id);

        return ResponseEntity.ok(ApiResponse.success("Edit Address Information", userAddress));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AddressResponse>> createAddress(
        @AuthenticationPrincipal CustomUserDetails principal, 
        @Valid @RequestBody AddressRequest request
    ){
        AddressResponse response = addressService.createAddress(principal.getUser().getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Address Created Successfully", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
        @AuthenticationPrincipal CustomUserDetails principal, 
        @PathVariable Long id,
        @Valid @RequestBody AddressUpdateRequest request
    ){
        AddressResponse response = addressService.updateAddress(principal.getUser().getId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Address Update Successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteAddress(
        @AuthenticationPrincipal CustomUserDetails principal, 
        @PathVariable Long id
    ){
        String response = addressService.deleteAddress(principal.getUser().getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Address Deleted", response));
    }

    @PatchMapping("/{id}/default")
    public ResponseEntity<ApiResponse<AddressResponse>> setDefaultAddress(
        @AuthenticationPrincipal CustomUserDetails principal, 
        @PathVariable Long id
    ){
        AddressResponse response = addressService.setDefaultAddress(principal.getUser().getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Default address is set successfully", response));
    }

}
