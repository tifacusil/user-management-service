package com.landslide.usermanagement.controller;

import com.landslide.usermanagement.dto.LoginRequest;
import com.landslide.usermanagement.dto.LoginResponse;
import com.landslide.usermanagement.dto.RegisterRequest;
import com.landslide.usermanagement.dto.RegisterResponse;
import com.landslide.usermanagement.exception.UnauthorizedException;
import com.landslide.usermanagement.security.UserPrincipal;
import com.landslide.usermanagement.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Authentication", description = "Register and login — no JWT required")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(
            summary = "Register a new user",
            description = """
                    Creates a new user. Roles: `admin` · `operator` · `viewer`.

                    - An unauthenticated request can only create `operator` or `viewer` accounts.
                    - Creating an `admin` account requires an existing authenticated admin (pass the JWT).
                    """,
            responses = {
                    @ApiResponse(responseCode = "201", description = "User created",
                            content = @Content(schema = @Schema(implementation = RegisterResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Validation error",
                            content = @Content(examples = @ExampleObject(value = """
                                    {"error": "username must not be blank"}"""))),
                    @ApiResponse(responseCode = "403", description = "Non-admin trying to create admin",
                            content = @Content(examples = @ExampleObject(value = """
                                    {"error": "Only admins can create admin accounts"}"""))),
                    @ApiResponse(responseCode = "409", description = "Username or email already exists",
                            content = @Content(examples = @ExampleObject(value = """
                                    {"error": "Username already exists"}""")))
            }
    )
    @SecurityRequirements   // mark as not requiring auth in Swagger UI
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest req) {
        UserPrincipal caller = getCallerOrNull();
        RegisterResponse response = authService.register(req, caller);
        return ResponseEntity.status(201).body(response);
    }

    @Operation(
            summary = "Login and obtain a JWT",
            description = "Returns a signed JWT valid for 24 hours. The same 401 message is returned for both wrong password and unknown username (timing-safe).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Authenticated — JWT returned",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = LoginResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Invalid credentials",
                            content = @Content(examples = @ExampleObject(value = """
                                    {"error": "Invalid username or password"}""")))
            }
    )
    @SecurityRequirements
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
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
