package com.landslide.usermanagement.dto;

import jakarta.validation.constraints.NotBlank;

public class DeviceRegisterRequest {

    @NotBlank
    private String deviceToken;

    @NotBlank
    private String platform;

    private String appVersion;

    public String getDeviceToken() { return deviceToken; }
    public void setDeviceToken(String deviceToken) { this.deviceToken = deviceToken; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public String getAppVersion() { return appVersion; }
    public void setAppVersion(String appVersion) { this.appVersion = appVersion; }
}
