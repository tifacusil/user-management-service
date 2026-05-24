package com.landslide.usermanagement.security;

/**
 * Holds authenticated user info extracted from JWT.
 * Stored as the principal in the Spring Security context.
 */
public class UserPrincipal {

    private final Long userId;
    private final String username;
    private final String role;

    public UserPrincipal(Long userId, String username, String role) {
        this.userId = userId;
        this.username = username;
        this.role = role;
    }

    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getRole() { return role; }

    public boolean isAdmin() { return "admin".equalsIgnoreCase(role); }
}
