package com.landslide.usermanagement.controller;

import com.landslide.usermanagement.dto.SubscriptionRequest;
import com.landslide.usermanagement.dto.SubscriptionResponse;
import com.landslide.usermanagement.exception.ForbiddenException;
import com.landslide.usermanagement.security.UserPrincipal;
import com.landslide.usermanagement.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Subscriptions", description = "Alert subscription management — admin only for creation")
@RestController
@RequestMapping("/api/v1/users")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @Operation(
            summary = "List all subscriptions",
            description = "**Admin only.** Returns every row in the subscriptions table.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Full subscription list returned"),
                    @ApiResponse(responseCode = "403", description = "Caller is not an admin",
                            content = @Content(examples = @ExampleObject(value = """
                                    {"error": "Only admins can view all subscriptions"}""")))
            }
    )
    @GetMapping("/subscriptions")
    public ResponseEntity<List<SubscriptionResponse>> getAllSubscriptions(
            @AuthenticationPrincipal UserPrincipal caller) {

        if (!caller.isAdmin()) {
            throw new ForbiddenException("Only admins can view all subscriptions");
        }
        return ResponseEntity.ok(subscriptionService.getAllSubscriptions());
    }

    @Operation(
            summary = "Subscribe a user to project alerts",
            description = """
                    **Admin only.** Subscribes the user to receive alerts from a project at or above
                    the specified minimum level. Alert levels: `blue` < `orange` < `red`.

                    `quiet_hours_start` / `quiet_hours_end` are optional. The window can cross midnight
                    (e.g. `22:00` → `06:00`). Red alerts always bypass quiet hours.
                    """,
            parameters = @Parameter(name = "userId", description = "Target user ID", required = true, example = "1"),
            responses = {
                    @ApiResponse(responseCode = "201", description = "Subscription created",
                            content = @Content(schema = @Schema(implementation = SubscriptionResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Caller is not an admin",
                            content = @Content(examples = @ExampleObject(value = """
                                    {"error": "Only admins can manage subscriptions"}"""))),
                    @ApiResponse(responseCode = "404", description = "User or project not found"),
                    @ApiResponse(responseCode = "409", description = "Subscription already exists")
            }
    )
    @PostMapping("/{userId}/subscriptions")
    public ResponseEntity<SubscriptionResponse> createSubscription(
            @PathVariable Long userId,
            @Valid @RequestBody SubscriptionRequest req,
            @AuthenticationPrincipal UserPrincipal caller) {

        if (!caller.isAdmin()) {
            throw new ForbiddenException("Only admins can manage subscriptions");
        }
        return ResponseEntity.status(201).body(subscriptionService.createSubscription(userId, req));
    }

    @Operation(
            summary = "List subscriptions for a user",
            description = "Admins can list any user's subscriptions. Operators/viewers can only list their own.",
            parameters = @Parameter(name = "userId", description = "Target user ID", required = true, example = "1"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Subscription list returned"),
                    @ApiResponse(responseCode = "403", description = "Accessing another user's subscriptions without admin role")
            }
    )
    @GetMapping("/{userId}/subscriptions")
    public ResponseEntity<List<SubscriptionResponse>> getSubscriptions(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserPrincipal caller) {

        if (!caller.isAdmin() && !caller.getUserId().equals(userId)) {
            throw new ForbiddenException("Access denied");
        }
        return ResponseEntity.ok(subscriptionService.getSubscriptions(userId));
    }
}
