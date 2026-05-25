package com.landslide.usermanagement.controller;

import com.landslide.usermanagement.dto.AlertRecipientsResponse;
import com.landslide.usermanagement.service.AlertRecipientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Alert Recipients", description = "Consumed by the Early Warning service to resolve who to notify")
@RestController
@RequestMapping("/api/v1/projects")
public class AlertRecipientController {

    private final AlertRecipientService alertRecipientService;

    public AlertRecipientController(AlertRecipientService alertRecipientService) {
        this.alertRecipientService = alertRecipientService;
    }

    @Operation(
            summary = "Get alert recipients for a project",
            description = """
                    Returns all active users who should be notified for an alert at the given level,
                    with per-channel details (SMS, push, email).

                    **Filtering rules:**
                    1. Only users with `status = active`.
                    2. Only subscriptions where `min_alert_level ≤ requested level` (blue=1, orange=2, red=3).
                    3. `suppressed_by_quiet_hours = true` when current UTC time is inside the user's quiet
                       window **and** `level ≠ red`. Red alerts are **never** suppressed.
                    4. Quiet-hour windows crossing midnight (e.g. `22:00`–`06:00`) are handled correctly.
                    5. Only device tokens with `status = active` appear in `push.devices`.
                    6. Empty `recipients` → `[]` with HTTP 200 (not an error).
                    """,
            parameters = {
                    @Parameter(name = "projectId", description = "Project identifier", required = true, example = "shangtan"),
                    @Parameter(name = "level", description = "Alert level to query", required = true,
                            schema = @Schema(allowableValues = {"blue", "orange", "red"}), example = "orange")
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Recipients resolved",
                            content = @Content(schema = @Schema(implementation = AlertRecipientsResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid alert level")
            }
    )
    @GetMapping("/{projectId}/alert-recipients")
    public ResponseEntity<AlertRecipientsResponse> getAlertRecipients(
            @PathVariable String projectId,
            @RequestParam String level) {

        if (!level.equalsIgnoreCase("blue") &&
            !level.equalsIgnoreCase("orange") &&
            !level.equalsIgnoreCase("red")) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(
                alertRecipientService.getRecipients(projectId, level.toLowerCase()));
    }
}
