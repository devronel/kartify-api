package com.kartify.api.auth.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.kartify.api.auth.dto.AuthResponse;
import com.kartify.api.auth.dto.LoginRequest;
import com.kartify.api.auth.dto.PasswordResetRequest;
import com.kartify.api.auth.dto.RegisterRequest;
import com.kartify.api.exception.FieldValidationException;
import com.kartify.api.exception.PasswordResetException;
import com.kartify.api.exception.ResourceNotFoundException;
import com.kartify.api.security.CustomUserDetails;
import com.kartify.api.service.EmailService;
import com.kartify.api.user.entity.PasswordResetToken;
import com.kartify.api.user.entity.User;
import com.kartify.api.user.entity.UserDetail;
import com.kartify.api.user.entity.UserFile;
import com.kartify.api.user.enums.FileType;
import com.kartify.api.user.repository.PasswordResetTokenRepository;
import com.kartify.api.user.repository.UserFileRepository;
import com.kartify.api.user.repository.UserRepository;

@Service
public class AuthService {
    
    private final UserRepository userRepository;
    private final UserFileRepository userFileRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TemplateEngine templateEngine;
    private final EmailService emailService;

    @Value("${FRONTEND_URL}")
    private String frontendUrl;

    @Value("${imagekit.public-url}")
    private String imagekitUrl;

    public AuthService(
        UserRepository userRepository,
        UserFileRepository userFileRepository,
        PasswordResetTokenRepository passwordResetTokenRepository,
        PasswordEncoder passwordEncoder,
        AuthenticationManager authenticationManager,
        TemplateEngine templateEngine,
        EmailService emailService
    ){
        this.userRepository = userRepository;
        this.userFileRepository = userFileRepository;
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

        UserFile profilePicture = userFileRepository.findByUserIdAndType(saved.getId(), FileType.PROFILE_PICTURE)
            .orElse(null);

        return this.buildAuthResponse(user, profilePicture);
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

        UserFile profilePicture = userFileRepository.findByUserIdAndType(user.getId(), FileType.PROFILE_PICTURE)
            .orElse(null);

        return this.buildAuthResponse(user, profilePicture);
    }

    // --- Get authenticated user information ---
    public AuthResponse getUser(CustomUserDetails principal){

        User user = userRepository.findById(principal.getId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserFile profilePicture = userFileRepository.findByUserIdAndType(user.getId(), FileType.PROFILE_PICTURE)
            .orElse(null);

        return this.buildAuthResponse(user, profilePicture);
    }

    // --- Forgot Password ---
    public String forgotPassword(String email)
    {
        Optional<User> userCheck = userRepository.findByEmail(email);
        if(userCheck.isEmpty()){
            return "If an account with that email exists, a password reset link has been sent.";
        }

        User user = userCheck.get();

        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setEmail(user.getEmail());
        resetToken.setToken(token);
        resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(30));

        passwordResetTokenRepository.save(resetToken);

        Context context = new Context();
        context.setVariable("firstName", user.getUserDetail().getFirstName());
        context.setVariable("expiryMinutes", 15);
        context.setVariable("resetUrl",  frontendUrl + "/reset-password?email=" + user.getEmail() + "&token=" + token);
        String htmlContent = templateEngine.process("password-reset-email", context);

        emailService.sendEmail(
            user.getEmail(),
            "Password Reset Link",
            htmlContent
        );

        return "If an account with that email exists, a password reset link has been sent.";
    }

    // --- Change password ---
    @Transactional
    public String resetPasswordWithToken(PasswordResetRequest request){

        if (!request.password().equals(request.confirmPassword())) {
            throw new FieldValidationException("password", "Password do not match");
        }

        // --- Check if the email and token exists ---
        PasswordResetToken resetToken = passwordResetTokenRepository.findByEmailAndToken(request.email(), request.token())
            .orElseThrow(() ->
                new PasswordResetException("Invalid token.")
            );

        // --- Check if the token is aleady expired ---
        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new PasswordResetException("Token has expired.");
        }

        // --- Check if token is already used ---
        if (resetToken.isUsed()) {
            throw new PasswordResetException("Token already used.");
        }

        // --- Check the email if have exisiting user ---
        User user = userRepository.findByEmail(request.email()).orElseThrow(() ->
            new FieldValidationException("email", "Email not found.")
        );

        // --- Hash the new password and save ---
        user.setPassword(passwordEncoder.encode(request.password()));
        userRepository.save(user);

        // --- Invalidate the token to prevent reuse ---
        resetToken.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(resetToken);

        return "Password changed successfully";
    }

    // --- Validate Password Reset Token ---
    public String validatePasswordResetToken(String email, String token){
        PasswordResetToken resetToken = passwordResetTokenRepository.findByEmailAndToken(email, token)
            .orElseThrow(() ->
                new FieldValidationException("token", "Invalid token.")
            );

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new FieldValidationException("token", "Token has expired.");
        }

        if (resetToken.isUsed()) {
            throw new FieldValidationException("token", "Token already used.");
        }

        return "Token is still available.";
    }
    
    private AuthResponse buildAuthResponse(User user, UserFile profileImage){

        String profileImageUrl = profileImage != null
            ? imagekitUrl + "/profile/" + profileImage.getFilename()
            : null;

        return new AuthResponse(
            user.getId(),
            user.getEmail(),
            user.getFullName(),
            profileImageUrl,
            user.getRole()
        );
    }
}
