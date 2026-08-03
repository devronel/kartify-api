package com.kartify.api.auth.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kartify.api.auth.dto.AuthResponse;
import com.kartify.api.auth.dto.LoginRequest;
import com.kartify.api.auth.dto.RegisterRequest;
import com.kartify.api.auth.service.AuthService;
import com.kartify.api.security.CustomUserDetails;
import com.kartify.api.shared.dto.ApiResponse;
import com.kartify.api.user.entity.User;
import com.kartify.api.user.entity.UserDetail;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class AuthController {
    
    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success("Account Created!", response));
    }

    @PostMapping("/authenticate")
    public ResponseEntity<ApiResponse<AuthResponse>> authenticate(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.authenticate(request);
        return ResponseEntity.ok(ApiResponse.success("Login Successful!", response));
    }

    @GetMapping("/user")
    public ResponseEntity<AuthResponse> getUser(@AuthenticationPrincipal CustomUserDetails principal){
        AuthResponse user = authService.getUser(principal);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/csrf-cookie")
    public ResponseEntity<Void> csrf() {
        return ResponseEntity.noContent().build();
    }
}
