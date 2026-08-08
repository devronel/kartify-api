package com.kartify.api.auth.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kartify.api.auth.dto.AuthResponse;
import com.kartify.api.auth.dto.LoginRequest;
import com.kartify.api.auth.dto.PasswordResetRequest;
import com.kartify.api.auth.dto.RegisterRequest;
import com.kartify.api.auth.service.AuthService;
import com.kartify.api.security.CustomUserDetails;
import com.kartify.api.shared.dto.ApiResponse;

import jakarta.mail.MessagingException;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class AuthController {
    
    private final AuthService authService;

    public AuthController(AuthService authService){
        this.authService = authService;
    }

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
    public ResponseEntity<ApiResponse<AuthResponse>> getUser(@AuthenticationPrincipal CustomUserDetails principal){
        AuthResponse user = authService.getUser(principal);
        return ResponseEntity.ok(ApiResponse.success("Authenticated User", user));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@RequestBody Map<String, Object> request) {
        String email = (String) request.get("email");
        String sendEmail = authService.forgotPassword(email);
        return ResponseEntity.ok(ApiResponse.success(sendEmail, null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPasswordWithToken(@Valid @RequestBody PasswordResetRequest request){
        String passwordChanged = authService.resetPasswordWithToken(request);
        return ResponseEntity.ok(passwordChanged);
    }

    @PostMapping("/validate-reset-token")
    public ResponseEntity<ApiResponse<String>> ValidatePasswordResetToken(@RequestBody Map<String, Object> request) {
        System.out.println(request);
        String email = (String) request.get("email");
        String token = (String) request.get("token");
        String validateResetToken = authService.validatePasswordResetToken(email, token);
        return ResponseEntity.ok(ApiResponse.success(validateResetToken, null));
    }

    @GetMapping("/csrf-cookie")
    public ResponseEntity<Void> csrf() {
        return ResponseEntity.noContent().build();
    }
}
