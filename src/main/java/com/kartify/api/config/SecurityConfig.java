package com.kartify.api.config;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // --- Password hashing — Spring's equivalent of Laravel's bcrypt() default ---
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // --- Exposes AuthenticationManager so AuthService can call authenticate() during login ---
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // --- CORS — allow Next.js (localhost:3000) to send credentialed requests ---
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true); // critical — allows the session cookie to be sent cross-origin

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
            )
            .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
            .securityContext(context -> context
                .requireExplicitSave(false)
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )
            .exceptionHandling(ex -> ex
                .accessDeniedHandler(accessDeniedHandler())
            )
            .logout(logout -> logout
                .logoutUrl("/api/logout")
                .logoutSuccessHandler((req, res, auth) -> {
                    System.out.println("Logout succesfull");
                    res.setStatus(HttpServletResponse.SC_OK); // Stop the 302 redirect, send 200 OK
                })
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/register", 
                    "/api/authenticate", 
                    "/api/csrf-cookie", 
                    "/api/forgot-password",
                    "/api/reset-password",
                    "/api/validate-reset-token"
                ).permitAll()
                .requestMatchers("/api/products/**", "/api/categories/**").permitAll()
                .anyRequest().authenticated()
            );

        return http.build();
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, ex) -> {

            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            Map<String, String> fieldErrors = new HashMap<>();

            if (ex instanceof InvalidCsrfTokenException) {
                fieldErrors.put("_csrf", "CSRF Token mismatch!.");
            } else if (ex instanceof MissingCsrfTokenException) {
                fieldErrors.put("_csrf", "Missing CSRF token. Please refresh the page and try again.");
            } else {
                fieldErrors.put("access", "Access denied.");
            }

            Map<String, Object> body = new HashMap<>();
            body.put("timestamp", LocalDateTime.now());
            body.put("status", HttpStatus.FORBIDDEN.value());
            body.put("message", "Validation failed");
            body.put("errors", fieldErrors);

            new ObjectMapper().writeValue(response.getOutputStream(), body);
        };
    }

}