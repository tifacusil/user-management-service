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
                // Same user re-registering the same token — just refresh last_seen_at
                deviceMapper.updateLastSeen(req.getDeviceToken());
                return Map.of("device_id", existing.getId(), "_status", 200);
            } else {
                // Token belongs to another user — revoke caller's stale tokens, then
                // transfer this token in-place to respect the UNIQUE constraint.
                deviceMapper.revokeByUserIdAndPlatform(userId, req.getPlatform());
                deviceMapper.transferToUser(
                        req.getDeviceToken(), userId,
                        req.getPlatform(), req.getAppVersion());
                return Map.of("device_id", existing.getId(), "_status", 201);
            }
        }

        // New token — drop any stale tokens the user already has before inserting
        deviceMapper.revokeByUserIdAndPlatform(userId, req.getPlatform());

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
