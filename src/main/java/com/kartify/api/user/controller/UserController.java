package com.kartify.api.user.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kartify.api.security.CustomUserDetails;
import com.kartify.api.shared.dto.ApiResponse;
import com.kartify.api.user.dto.UserDetailRequest;
import com.kartify.api.user.dto.UserDetailResponse;
import com.kartify.api.user.service.UserDetailService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserDetailService userDetailService;

    @PutMapping("/details")
    public ApiResponse<UserDetailResponse> updateMyDetails(@AuthenticationPrincipal CustomUserDetails principal, @Valid @RequestBody UserDetailRequest request) {
            
        UserDetailResponse user = userDetailService.upsertDetail(principal.getUser().getId(), request);

        return ApiResponse.success("User Details Changed.", user);
    }

    @GetMapping("/details")
    public ApiResponse<UserDetailResponse> updateMyDetails(@AuthenticationPrincipal CustomUserDetails principal) {
            
        UserDetailResponse user = userDetailService.getUserDetail(principal.getUser().getId());

        return ApiResponse.success("User Details Information.", user);
    }

}
