package com.kartify.api.auth.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.kartify.api.auth.dto.AuthResponse;
import com.kartify.api.auth.dto.LoginRequest;
import com.kartify.api.auth.dto.RegisterRequest;
import com.kartify.api.exception.FieldValidationException;
import com.kartify.api.security.CustomUserDetails;
import com.kartify.api.service.EmailService;
import com.kartify.api.user.entity.PasswordResetToken;
import com.kartify.api.user.entity.User;
import com.kartify.api.user.entity.UserDetail;
import com.kartify.api.user.repository.PasswordResetTokenRepository;
import com.kartify.api.user.repository.UserRepository;

import jakarta.mail.MessagingException;

@Service
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TemplateEngine templateEngine;
    private final EmailService emailService;

    @Value("${FRONTEND_URL}")
    private String frontendUrl;

    public AuthService(
        UserRepository userRepository,
        PasswordResetTokenRepository passwordResetTokenRepository,
        PasswordEncoder passwordEncoder,
        AuthenticationManager authenticationManager,
        TemplateEngine templateEngine,
        EmailService emailService
    ){
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.templateEngine = templateEngine;
        this.emailService = emailService;
    }

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

    // --- Forgot Password ---
    public String forgotPassword(String email) throws MessagingException
    {
        User user = userRepository.findByEmail(email).orElseThrow(() ->
            new FieldValidationException("email", "Email not found.")
        );

        String token = UUID.randomUUID().toString();

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setEmail(user.getEmail());
        resetToken.setToken(token);
        resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(15));

        passwordResetTokenRepository.save(resetToken);

        Context context = new Context();
        context.setVariable("firstName", user.getUserDetail().getFirstName());
        context.setVariable("expiryMinutes", 15);
        context.setVariable("resetUrl",  frontendUrl + "/reset-password?token=" + token);
        String htmlContent = templateEngine.process("password-reset-email", context);

        emailService.sendEmail(
            user.getEmail(),
            "Password Reset Link",
            htmlContent
        );

        return "Password reset sent to your email.";
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
