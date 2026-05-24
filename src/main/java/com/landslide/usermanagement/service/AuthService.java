package com.landslide.usermanagement.service;

import com.landslide.usermanagement.dto.LoginRequest;
import com.landslide.usermanagement.dto.LoginResponse;
import com.landslide.usermanagement.dto.RegisterRequest;
import com.landslide.usermanagement.dto.RegisterResponse;
import com.landslide.usermanagement.exception.DuplicateUserException;
import com.landslide.usermanagement.exception.ForbiddenException;
import com.landslide.usermanagement.exception.UnauthorizedException;
import com.landslide.usermanagement.mapper.UserMapper;
import com.landslide.usermanagement.model.User;
import com.landslide.usermanagement.security.JwtUtil;
import com.landslide.usermanagement.security.UserPrincipal;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserMapper userMapper,
                       BCryptPasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public RegisterResponse register(RegisterRequest req, UserPrincipal caller) {
        // Only admin can create another admin
        if ("admin".equalsIgnoreCase(req.getRole())) {
            if (caller == null || !caller.isAdmin()) {
                throw new ForbiddenException(
                        "Only an admin can create another admin user");
            }
        }

        // Validate role value
        String role = req.getRole() == null ? "operator" : req.getRole().toLowerCase();
        if (!role.equals("admin") && !role.equals("operator") && !role.equals("viewer")) {
            role = "operator";
        }

        // Check uniqueness
        if (userMapper.findByUsername(req.getUsername()) != null) {
            throw new DuplicateUserException("Username already exists: " + req.getUsername());
        }
        if (userMapper.findByEmail(req.getEmail()) != null) {
            throw new DuplicateUserException("Email already exists: " + req.getEmail());
        }

        User user = new User();
        user.setUsername(req.getUsername());
        user.setEmail(req.getEmail());
        user.setPhoneE164(req.getPhoneE164());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setRole(role);
        user.setStatus("active");

        userMapper.insert(user);

        return new RegisterResponse(user.getId(), user.getUsername(), user.getRole());
    }

    public LoginResponse login(LoginRequest req) {
        // Same error for non-existent user and wrong password (don't reveal which)
        User user = userMapper.findByUsername(req.getUsername());
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        Instant expiry = jwtUtil.getExpiryInstant();
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        String expiresAt = DateTimeFormatter.ISO_INSTANT.format(expiry);

        return new LoginResponse(token, user.getId(), user.getUsername(), user.getRole(), expiresAt);
    }
}
