package com.landslide.usermanagement.dto;

public class UpdateUserRequest {
    private String phoneE164;
    private String email;

    public String getPhoneE164() { return phoneE164; }
    public void setPhoneE164(String phoneE164) { this.phoneE164 = phoneE164; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
