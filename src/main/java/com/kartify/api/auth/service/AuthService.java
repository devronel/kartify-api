package com.kartify.api.auth.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.kartify.api.auth.dto.AuthResponse;
import com.kartify.api.auth.dto.RegisterRequest;
import com.kartify.api.user.entity.User;
import com.kartify.api.user.entity.UserDetail;
import com.kartify.api.user.repository.UserRepository;

@Service
public class AuthService {
    
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public AuthResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email is already registered");
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

        return new AuthResponse(saved.getId(), saved.getEmail());
        
    }
}
