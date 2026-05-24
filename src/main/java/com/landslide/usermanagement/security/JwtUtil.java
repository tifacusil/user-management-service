package com.landslide.usermanagement.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expirationHours;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-hours:24}") long expirationHours) {
        this.expirationHours = expirationHours;
        // HS256 requires >= 32 bytes; pad with zeros if needed
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            keyBytes = Arrays.copyOf(keyBytes, 32);
        }
        this.key = new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    public String generateToken(Long userId, String username, String role) {
        Instant now = Instant.now();
        Instant expiry = now.plus(expirationHours, ChronoUnit.HOURS);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Returns the expiry instant for a freshly-issued token.
     */
    public Instant getExpiryInstant() {
        return Instant.now().plus(expirationHours, ChronoUnit.HOURS);
    }

    /**
     * Parses and validates a JWT. Returns null if invalid or expired.
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public UserPrincipal principalFromClaims(Claims claims) {
        Long userId = Long.parseLong(claims.getSubject());
        String username = claims.get("username", String.class);
        String role = claims.get("role", String.class);
        return new UserPrincipal(userId, username, role);
    }
}
