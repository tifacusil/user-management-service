package com.landslide.usermanagement.dto;

import com.landslide.usermanagement.model.UserProjectAlertSubscription;

public class SubscriptionResponse {
    private Long id;
    private String projectId;
    private String minAlertLevel;
    private boolean smsEnabled;
    private boolean pushEnabled;
    private boolean emailEnabled;
    private String quietHoursStart;
    private String quietHoursEnd;

    public static SubscriptionResponse from(UserProjectAlertSubscription s) {
        SubscriptionResponse r = new SubscriptionResponse();
        r.id = s.getId();
        r.projectId = s.getProjectId();
        r.minAlertLevel = s.getMinAlertLevel();
        r.smsEnabled = s.isSmsEnabled();
        r.pushEnabled = s.isPushEnabled();
        r.emailEnabled = s.isEmailEnabled();
        // Normalize to HH:MM if DB returns HH:MM:SS
        r.quietHoursStart = normalizeTime(s.getQuietHoursStart());
        r.quietHoursEnd = normalizeTime(s.getQuietHoursEnd());
        return r;
    }

    private static String normalizeTime(String t) {
        if (t == null) return null;
        return t.length() > 5 ? t.substring(0, 5) : t;
    }

    public Long getId() { return id; }
    public String getProjectId() { return projectId; }
    public String getMinAlertLevel() { return minAlertLevel; }
    public boolean isSmsEnabled() { return smsEnabled; }
    public boolean isPushEnabled() { return pushEnabled; }
    public boolean isEmailEnabled() { return emailEnabled; }
    public String getQuietHoursStart() { return quietHoursStart; }
    public String getQuietHoursEnd() { return quietHoursEnd; }
}
