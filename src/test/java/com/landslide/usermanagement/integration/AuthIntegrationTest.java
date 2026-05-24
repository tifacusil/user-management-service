package com.landslide.usermanagement.integration;

import com.landslide.usermanagement.dto.LoginRequest;
import com.landslide.usermanagement.dto.LoginResponse;
import com.landslide.usermanagement.dto.RegisterRequest;
import com.landslide.usermanagement.dto.RegisterResponse;
import com.landslide.usermanagement.mapper.UserMapper;
import com.landslide.usermanagement.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private UserMapper userMapper;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private Long createdUserId;

    @BeforeEach
    void configureRestTemplate() {
        // Use Apache HttpClient5 instead of Java's HttpURLConnection.
        // HttpURLConnection throws HttpRetryException on 401 when the request
        // body was sent in streaming mode — Apache HttpClient5 handles 401 cleanly.
        rest.getRestTemplate().setRequestFactory(new HttpComponentsClientHttpRequestFactory());
    }

    @AfterEach
    void cleanup() {
        // No direct delete mapper needed — just note test user was created.
        // In a real CI environment, use a dedicated test DB that gets wiped.
    }

    // -------------------------------------------------------------------------
    // Test 1: POST /auth/register → user created, password is hashed
    // -------------------------------------------------------------------------
    @Test
    void register_createsUser_withHashedPassword() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("test_register_" + System.currentTimeMillis());
        req.setEmail("test_" + System.currentTimeMillis() + "@test.com");
        req.setPassword("secret123");
        req.setRole("operator");

        ResponseEntity<RegisterResponse> response =
                rest.postForEntity("/api/v1/auth/register", req, RegisterResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getUsername()).isEqualTo(req.getUsername());
        assertThat(response.getBody().getRole()).isEqualTo("operator");
        assertThat(response.getBody().getUserId()).isNotNull();

        // Verify password is not stored as plain text
        User stored = userMapper.findByUsername(req.getUsername());
        assertThat(stored).isNotNull();
        assertThat(stored.getPasswordHash()).isNotEqualTo("secret123");
        assertThat(stored.getPasswordHash()).startsWith("$2a$");  // BCrypt prefix
    }

    // -------------------------------------------------------------------------
    // Test 2: POST /auth/login with correct credentials → valid JWT
    // -------------------------------------------------------------------------
    @Test
    void login_withCorrectCredentials_returnsJwtWithClaims() {
        // First register a user
        String username = "test_login_" + System.currentTimeMillis();
        String password = "password123";

        RegisterRequest reg = new RegisterRequest();
        reg.setUsername(username);
        reg.setEmail(username + "@test.com");
        reg.setPassword(password);
        reg.setRole("operator");
        rest.postForEntity("/api/v1/auth/register", reg, RegisterResponse.class);

        // Now login
        LoginRequest login = new LoginRequest();
        login.setUsername(username);
        login.setPassword(password);

        ResponseEntity<LoginResponse> response =
                rest.postForEntity("/api/v1/auth/login", login, LoginResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getToken()).isNotBlank();
        assertThat(response.getBody().getUsername()).isEqualTo(username);
        assertThat(response.getBody().getRole()).isEqualTo("operator");

        // Decode and verify JWT claims
        Claims claims = parseJwt(response.getBody().getToken());
        assertThat(claims.get("username", String.class)).isEqualTo(username);
        assertThat(claims.get("role", String.class)).isEqualTo("operator");
        assertThat(claims.getSubject()).isNotBlank(); // user_id
    }

    // -------------------------------------------------------------------------
    // Test 3: POST /auth/login with wrong password → 401
    // -------------------------------------------------------------------------
    @Test
    void login_withWrongPassword_returns401() {
        // Register first
        String username = "test_badpw_" + System.currentTimeMillis();
        RegisterRequest reg = new RegisterRequest();
        reg.setUsername(username);
        reg.setEmail(username + "@test.com");
        reg.setPassword("correctpassword");
        reg.setRole("operator");
        rest.postForEntity("/api/v1/auth/register", reg, RegisterResponse.class);

        LoginRequest login = new LoginRequest();
        login.setUsername(username);
        login.setPassword("wrongpassword");

        ResponseEntity<String> response =
                rest.postForEntity("/api/v1/auth/login", login, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // -------------------------------------------------------------------------
    // Helper: parse JWT using the same secret as the app
    // -------------------------------------------------------------------------
    private Claims parseJwt(String token) {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            keyBytes = Arrays.copyOf(keyBytes, 32);
        }
        SecretKey key = new SecretKeySpec(keyBytes, "HmacSHA256");
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
