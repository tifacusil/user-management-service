package com.landslide.usermanagement.dto;

public class RegisterResponse {
    private Long userId;
    private String username;
    private String role;

    public RegisterResponse(Long userId, String username, String role) {
        this.userId = userId;
        this.username = username;
        this.role = role;
    }

    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
}
