package com.kartify.api.auth.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.kartify.api.auth.dto.AuthResponse;
import com.kartify.api.auth.dto.LoginRequest;
import com.kartify.api.auth.dto.RegisterRequest;
import com.kartify.api.exception.FieldValidationException;
import com.kartify.api.security.CustomUserDetails;
import com.kartify.api.user.entity.User;
import com.kartify.api.user.entity.UserDetail;
import com.kartify.api.user.repository.UserRepository;

@Service
public class AuthService {
    
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    // --- Register ---
    public AuthResponse register(RegisterRequest request) {

        if (!request.password().equals(request.confirmPassword())) {
            throw new FieldValidationException("password", "Passwords do not match");
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new FieldValidationException("email", "Email is already registered");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));

        UserDetail userDetail = new UserDetail();
        userDetail.setFirstName(request.firstName());
        userDetail.setLastName(request.lastName());
        userDetail.setUser(user);

        user.setUserDetail(userDetail);

        User saved = userRepository.save(user);

        return this.buildAuthResponse(saved);
    }

    // --- Login ---
    public AuthResponse authenticate(LoginRequest request) {
        // This triggers CustomUserDetailsService + password check internally
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        // Store the authenticated user in the security context — this is what creates the session
        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalStateException("User not found after authentication"));

        return this.buildAuthResponse(user);
    }

    // --- Get authenticated user information ---
    public AuthResponse getUser(CustomUserDetails principal){
        User user = principal.getUser();
        return this.buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user){
        return new AuthResponse(
            user.getId(),
            user.getEmail(),
            user.getFullName(),
            user.getRole()
        );
    }
}
