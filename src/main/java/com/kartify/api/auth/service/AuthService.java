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

    @Value("${app.url-storefront}")
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

    
    /**
     * Create new user account
     * 
     * Include creation of user details to save the first and last name
     * of the user
     * 
     * @param request contains the required fields
     * @return auth response
     * @throws FieldValidationException if password and confirm password didn't match
     * @throws FieldValidationException if email already register
     */
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

    
    /**
     * Authenticate user
     * 
     * @param request contains user credentials
     * @return auth response
     * @throws ResourceNotFoundException if no user found
     */
    public AuthResponse authenticate(LoginRequest request) {
        
        // Authenticate the email and password by authentication manager
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        // Store the authenticated user in the security context to create session
        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new ResourceNotFoundException("User not found after authentication"));

        UserFile profilePicture = userFileRepository.findByUserIdAndType(user.getId(), FileType.PROFILE_PICTURE)
            .orElse(null);

        return this.buildAuthResponse(user, profilePicture);
    }

    
    /**
     * Get the authenticated user
     * 
     * 
     * @param principal contains the authenticated user information
     * @return auth response
     * @throws ResourceNotFoundException if no user found
     */
    public AuthResponse getUser(CustomUserDetails principal){

        User user = userRepository.findById(principal.getId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserFile profilePicture = userFileRepository.findByUserIdAndType(user.getId(), FileType.PROFILE_PICTURE)
            .orElse(null);

        return this.buildAuthResponse(user, profilePicture);
    }


    /**
     * Forgot password
     * 
     * Send the password reset link to the email in parameter
     * 
     * @param email
     * @return string message that the password already send to the email
     */
    public String forgotPassword(String email)
    {
        Optional<User> userCheck = userRepository.findByEmail(email);
        if(userCheck.isEmpty()){
            return "If an account with that email exists, a password reset link has been sent.";
        }

        User user = userCheck.get();

        // Generate random string to password reset token
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setEmail(user.getEmail());
        resetToken.setToken(token);
        resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(30));

        passwordResetTokenRepository.save(resetToken);


        // Generate the content body of the email
        Context context = new Context();
        context.setVariable("firstName", user.getUserDetail().getFirstName());
        context.setVariable("expiryMinutes", 15);
        context.setVariable("resetUrl",  frontendUrl + "/reset-password?email=" + user.getEmail() + "&token=" + token);
        String htmlContent = templateEngine.process("password-reset-email", context);

        emailService.sendHtmlEmail(
            user.getEmail(),
            "Password Reset Link",
            htmlContent
        );

        return "If an account with that email exists, a password reset link has been sent.";
    }

    
    /**
     * Reset password
     * 
     * Use of reset password link sent to the email.
     * 
     * It includes the email, token and the update password.
     * 
     * @param request contains the required fields
     * @return string
     * @throws FieldValidationException if password and confirm password didn't match
     * @throws PasswordResetException if the token is invalid
     * @throws PasswordResetException if the token is expired
     * @throws PasswordResetException if the token used already
     * @throws FieldValidationException if the email not found
     */
    @Transactional
    public String resetPasswordWithToken(PasswordResetRequest request){

        if (!request.password().equals(request.confirmPassword())) {
            throw new FieldValidationException("password", "Password do not match");
        }

        PasswordResetToken resetToken = passwordResetTokenRepository.findByEmailAndToken(request.email(), request.token())
            .orElseThrow(() ->
                new PasswordResetException("Invalid token.")
            );

        
        // Check if token is expired
        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new PasswordResetException("Token has expired.");
        }

        // Check if token is already used
        if (resetToken.isUsed()) {
            throw new PasswordResetException("Token already used.");
        }


        User user = userRepository.findByEmail(request.email()).orElseThrow(() ->
            new FieldValidationException("email", "Email not found.")
        );


        user.setPassword(passwordEncoder.encode(request.password()));
        userRepository.save(user);

        // Invalidate the token to prevent reuse
        resetToken.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(resetToken);

        return "Password changed successfully";
    }


    /**
     * Validate the password reset token
     * 
     * Verify the password reset link token and email if still available
     * 
     * @param email
     * @param token
     * @return string if token still available
     * @throws FieldValidationException if invalid token
     * @throws FieldValidationException if token is expired
     * @throws FieldValidationException if token already used
     */
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
