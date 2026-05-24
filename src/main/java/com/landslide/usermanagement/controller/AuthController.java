package com.landslide.usermanagement.controller;

import com.landslide.usermanagement.dto.LoginRequest;
import com.landslide.usermanagement.dto.LoginResponse;
import com.landslide.usermanagement.dto.RegisterRequest;
import com.landslide.usermanagement.dto.RegisterResponse;
import com.landslide.usermanagement.exception.UnauthorizedException;
import com.landslide.usermanagement.security.UserPrincipal;
import com.landslide.usermanagement.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest req) {
        // Extract optional caller from security context (may be anonymous)
        UserPrincipal caller = getCallerOrNull();
        RegisterResponse response = authService.register(req, caller);
        return ResponseEntity.status(201).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        // Handle inline to avoid Spring Security's WWW-Authenticate header on 401
        try {
            LoginResponse response = authService.login(req);
            return ResponseEntity.ok(response);
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    private UserPrincipal getCallerOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal up) {
            return up;
        }
        return null;
    }
}
