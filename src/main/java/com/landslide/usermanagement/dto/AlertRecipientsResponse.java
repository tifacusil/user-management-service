package com.landslide.usermanagement.dto;

import java.util.List;

public class AlertRecipientsResponse {
    private String projectId;
    private String alertLevel;
    private List<RecipientDto> recipients;
    private String generatedAt;

    public AlertRecipientsResponse(String projectId, String alertLevel,
                                   List<RecipientDto> recipients, String generatedAt) {
        this.projectId = projectId;
        this.alertLevel = alertLevel;
        this.recipients = recipients;
        this.generatedAt = generatedAt;
    }

    public String getProjectId() { return projectId; }
    public String getAlertLevel() { return alertLevel; }
    public List<RecipientDto> getRecipients() { return recipients; }
    public String getGeneratedAt() { return generatedAt; }

    // ---- nested DTOs ----

    public static class RecipientDto {
        private Long userId;
        private String username;
        private String role;
        private ChannelsDto channels;

        public RecipientDto(Long userId, String username, String role, ChannelsDto channels) {
            this.userId = userId;
            this.username = username;
            this.role = role;
            this.channels = channels;
        }

        public Long getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getRole() { return role; }
        public ChannelsDto getChannels() { return channels; }
    }

    public static class ChannelsDto {
        private SmsChannelDto sms;
        private PushChannelDto push;
        private EmailChannelDto email;

        public ChannelsDto(SmsChannelDto sms, PushChannelDto push, EmailChannelDto email) {
            this.sms = sms;
            this.push = push;
            this.email = email;
        }

        public SmsChannelDto getSms() { return sms; }
        public PushChannelDto getPush() { return push; }
        public EmailChannelDto getEmail() { return email; }
    }

    public static class SmsChannelDto {
        private boolean enabled;
        private String phoneE164;
        private boolean suppressedByQuietHours;

        public SmsChannelDto(boolean enabled, String phoneE164, boolean suppressedByQuietHours) {
            this.enabled = enabled;
            this.phoneE164 = phoneE164;
            this.suppressedByQuietHours = suppressedByQuietHours;
        }

        public boolean isEnabled() { return enabled; }
        public String getPhoneE164() { return phoneE164; }
        public boolean isSuppressedByQuietHours() { return suppressedByQuietHours; }
    }

    public static class PushChannelDto {
        private boolean enabled;
        private List<DeviceDto> devices;

        public PushChannelDto(boolean enabled, List<DeviceDto> devices) {
            this.enabled = enabled;
            this.devices = devices;
        }

        public boolean isEnabled() { return enabled; }
        public List<DeviceDto> getDevices() { return devices; }
    }

    public static class DeviceDto {
        private String token;
        private String platform;

        public DeviceDto(String token, String platform) {
            this.token = token;
            this.platform = platform;
        }

        public String getToken() { return token; }
        public String getPlatform() { return platform; }
    }

    public static class EmailChannelDto {
        private boolean enabled;
        private String address;

        public EmailChannelDto(boolean enabled, String address) {
            this.enabled = enabled;
            this.address = address;
        }

        public boolean isEnabled() { return enabled; }
        public String getAddress() { return address; }
    }
}
