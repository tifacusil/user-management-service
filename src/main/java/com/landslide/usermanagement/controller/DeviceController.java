package com.landslide.usermanagement.controller;

import com.landslide.usermanagement.dto.DeviceRegisterRequest;
import com.landslide.usermanagement.dto.RevokeDeviceRequest;
import com.landslide.usermanagement.security.UserPrincipal;
import com.landslide.usermanagement.service.DeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Devices", description = "Push notification device token registration and revocation")
@RestController
@RequestMapping("/api/v1/devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @Operation(
            summary = "Register or refresh a device token",
            description = """
                    Registers a push notification token for the authenticated user.

                    - **201** — new token registered.
                    - **200** — same user already owns this token; `last_seen_at` refreshed.
                    - If the token belongs to **another** user, ownership is transferred to the caller.

                    Platforms: `android` · `ios` · `web`
                    """,
            responses = {
                    @ApiResponse(responseCode = "201", description = "Token registered",
                            content = @Content(examples = @ExampleObject(value = """
                                    {"device_id": 5}"""))),
                    @ApiResponse(responseCode = "200", description = "Token refreshed (already owned by caller)",
                            content = @Content(examples = @ExampleObject(value = """
                                    {"device_id": 5}""")))
            }
    )
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerDevice(
            @Valid @RequestBody DeviceRegisterRequest req,
            @AuthenticationPrincipal UserPrincipal caller) {

        Map<String, Object> result = deviceService.registerDevice(caller.getUserId(), req);
        int status = (int) result.get("_status");
        Map<String, Object> body = Map.of("device_id", result.get("device_id"));
        return ResponseEntity.status(status).body(body);
    }

    @Operation(
            summary = "Revoke a device token",
            description = "Marks a token as revoked. Typically called by the Early Warning service when FCM reports a token as invalid (`fcm_unregistered`).",
            parameters = @Parameter(name = "deviceToken", description = "The FCM/APNs device token to revoke", required = true),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Token revoked",
                            content = @Content(examples = @ExampleObject(value = """
                                    {"revoked": true}"""))),
                    @ApiResponse(responseCode = "404", description = "Token not found",
                            content = @Content(examples = @ExampleObject(value = """
                                    {"error": "Device token not found"}""")))
            }
    )
    @PostMapping("/{deviceToken}/revoke")
    public ResponseEntity<Map<String, Boolean>> revokeDevice(
            @PathVariable String deviceToken,
            @RequestBody(required = false) RevokeDeviceRequest req) {

        deviceService.revokeDevice(deviceToken);
        return ResponseEntity.ok(Map.of("revoked", true));
    }
}
