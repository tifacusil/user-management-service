package com.landslide.usermanagement.model;

import java.time.LocalDateTime;

public class UserProjectAlertSubscription {
    private Long id;
    private Long userId;
    private String projectId;
    private String minAlertLevel;   // blue | orange | red
    private boolean smsEnabled;
    private boolean pushEnabled;
    private boolean emailEnabled;
    private String quietHoursStart; // "HH:MM" or "HH:MM:SS" from DB
    private String quietHoursEnd;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

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

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
