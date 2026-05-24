package com.landslide.usermanagement.controller;

import com.landslide.usermanagement.dto.AlertRecipientsResponse;
import com.landslide.usermanagement.service.AlertRecipientService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/projects")
public class AlertRecipientController {

    private final AlertRecipientService alertRecipientService;

    public AlertRecipientController(AlertRecipientService alertRecipientService) {
        this.alertRecipientService = alertRecipientService;
    }

    /**
     * GET /api/v1/projects/{projectId}/alert-recipients?level=orange
     * Consumed by the Early Warning service.
     */
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
