package com.landslide.usermanagement.dto;

public class LoginResponse {
    private String token;
    private Long userId;
    private String username;
    private String role;
    private String expiresAt;

    public LoginResponse(String token, Long userId, String username, String role, String expiresAt) {
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.expiresAt = expiresAt;
    }

    public String getToken() { return token; }
    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public String getExpiresAt() { return expiresAt; }
}
