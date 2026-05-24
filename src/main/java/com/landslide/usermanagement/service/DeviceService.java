package com.landslide.usermanagement.service;

import com.landslide.usermanagement.dto.DeviceRegisterRequest;
import com.landslide.usermanagement.exception.DeviceNotFoundException;
import com.landslide.usermanagement.mapper.DeviceMapper;
import com.landslide.usermanagement.model.UserDevice;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class DeviceService {

    private final DeviceMapper deviceMapper;

    public DeviceService(DeviceMapper deviceMapper) {
        this.deviceMapper = deviceMapper;
    }

    /**
     * Registers a device token for the calling user.
     * Returns a map with "device_id" and "status" (201 or 200).
     */
    public Map<String, Object> registerDevice(Long userId, DeviceRegisterRequest req) {
        UserDevice existing = deviceMapper.findByToken(req.getDeviceToken());

        if (existing != null) {
            if (existing.getUserId().equals(userId)) {
                // Same user — just refresh last_seen_at
                deviceMapper.updateLastSeen(req.getDeviceToken());
                return Map.of("device_id", existing.getId(), "_status", 200);
            } else {
                // Different user — transfer token ownership.
                // We update-in-place to respect any UNIQUE constraint on device_token.
                // The previous owner effectively loses the token (semantically "revoked").
                deviceMapper.transferToUser(
                        req.getDeviceToken(), userId,
                        req.getPlatform(), req.getAppVersion());
                return Map.of("device_id", existing.getId(), "_status", 201);
            }
        }

        UserDevice device = new UserDevice();
        device.setUserId(userId);
        device.setDeviceToken(req.getDeviceToken());
        device.setPlatform(req.getPlatform());
        device.setAppVersion(req.getAppVersion());
        deviceMapper.insert(device);

        return Map.of("device_id", device.getId(), "_status", 201);
    }

    public void revokeDevice(String deviceToken) {
        UserDevice device = deviceMapper.findByToken(deviceToken);
        if (device == null) {
            throw new DeviceNotFoundException("Device token not found: " + deviceToken);
        }
        deviceMapper.revokeByToken(deviceToken);
    }
}
