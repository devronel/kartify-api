package com.kartify.api.auth.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kartify.api.auth.dto.AuthResponse;
import com.kartify.api.auth.dto.LoginRequest;
import com.kartify.api.auth.dto.PasswordResetRequest;
import com.kartify.api.auth.dto.RegisterRequest;
import com.kartify.api.auth.service.AuthService;
import com.kartify.api.security.CustomUserDetails;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class AuthController {
    
    private final AuthService authService;

    public AuthController(AuthService authService){
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/authenticate")
    public ResponseEntity<AuthResponse> authenticate(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.authenticate(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user")
    public ResponseEntity<AuthResponse> getUser(@AuthenticationPrincipal CustomUserDetails principal){
        AuthResponse user = authService.getUser(principal);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody Map<String, Object> request) {
        String email = (String) request.get("email");
        String sendEmail = authService.forgotPassword(email);
        return ResponseEntity.ok(sendEmail);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPasswordWithToken(@Valid @RequestBody PasswordResetRequest request){
        String passwordChanged = authService.resetPasswordWithToken(request);
        return ResponseEntity.ok(passwordChanged);
    }

    @PostMapping("/validate-reset-token")
    public ResponseEntity<String> ValidatePasswordResetToken(@RequestBody Map<String, Object> request) {
        String email = (String) request.get("email");
        String token = (String) request.get("token");
        String validateResetToken = authService.validatePasswordResetToken(email, token);
        return ResponseEntity.ok(validateResetToken);
    }

    @GetMapping("/csrf-cookie")
    public ResponseEntity<Void> csrf() {
        return ResponseEntity.noContent().build();
    }
}
