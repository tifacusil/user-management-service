package com.landslide.usermanagement.dto;

import com.landslide.usermanagement.model.User;

import java.time.format.DateTimeFormatter;

public class UserResponse {
    private Long userId;
    private String username;
    private String email;
    private String phoneE164;
    private String role;
    private String status;
    private String createdAt;

    public static UserResponse from(User user) {
        UserResponse r = new UserResponse();
        r.userId = user.getId();
        r.username = user.getUsername();
        r.email = user.getEmail();
        r.phoneE164 = user.getPhoneE164();
        r.role = user.getRole();
        r.status = user.getStatus();
        r.createdAt = user.getCreatedAt() != null
                ? user.getCreatedAt().format(DateTimeFormatter.ISO_DATE_TIME) + "Z"
                : null;
        return r;
    }

    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPhoneE164() { return phoneE164; }
    public String getRole() { return role; }
    public String getStatus() { return status; }
    public String getCreatedAt() { return createdAt; }
}
