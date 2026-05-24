package com.landslide.usermanagement.dto;

import jakarta.validation.constraints.NotBlank;

public class SubscriptionRequest {

    @NotBlank
    private String projectId;

    @NotBlank
    private String minAlertLevel;

    private boolean smsEnabled;
    private boolean pushEnabled;
    private boolean emailEnabled;
    private String quietHoursStart;  // "HH:MM", nullable
    private String quietHoursEnd;    // "HH:MM", nullable

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getMinAlertLevel() { return minAlertLevel; }
    public void setMinAlertLevel(String minAlertLevel) { this.minAlertLevel = minAlertLevel; }

    public boolean isSmsEnabled() { return smsEnabled; }
    public void setSmsEnabled(boolean smsEnabled) { this.smsEnabled = smsEnabled; }

    public boolean isPushEnabled() { return pushEnabled; }
    public void setPushEnabled(boolean pushEnabled) { this.pushEnabled = pushEnabled; }

    public boolean isEmailEnabled() { return emailEnabled; }
    public void setEmailEnabled(boolean emailEnabled) { this.emailEnabled = emailEnabled; }

    public String getQuietHoursStart() { return quietHoursStart; }
    public void setQuietHoursStart(String quietHoursStart) { this.quietHoursStart = quietHoursStart; }

    public String getQuietHoursEnd() { return quietHoursEnd; }
    public void setQuietHoursEnd(String quietHoursEnd) { this.quietHoursEnd = quietHoursEnd; }
}
