package com.landslide.usermanagement.controller;

import com.landslide.usermanagement.dto.DeviceRegisterRequest;
import com.landslide.usermanagement.dto.RevokeDeviceRequest;
import com.landslide.usermanagement.security.UserPrincipal;
import com.landslide.usermanagement.service.DeviceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerDevice(
            @Valid @RequestBody DeviceRegisterRequest req,
            @AuthenticationPrincipal UserPrincipal caller) {

        Map<String, Object> result = deviceService.registerDevice(caller.getUserId(), req);
        int status = (int) result.get("_status");
        // Remove internal status key before returning
        Map<String, Object> body = Map.of("device_id", result.get("device_id"));
        return ResponseEntity.status(status).body(body);
    }

    @PostMapping("/{deviceToken}/revoke")
    public ResponseEntity<Map<String, Boolean>> revokeDevice(
            @PathVariable String deviceToken,
            @RequestBody(required = false) RevokeDeviceRequest req) {

        deviceService.revokeDevice(deviceToken);
        return ResponseEntity.ok(Map.of("revoked", true));
    }
}
