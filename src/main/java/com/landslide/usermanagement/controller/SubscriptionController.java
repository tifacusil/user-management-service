package com.landslide.usermanagement.controller;

import com.landslide.usermanagement.dto.SubscriptionRequest;
import com.landslide.usermanagement.dto.SubscriptionResponse;
import com.landslide.usermanagement.exception.ForbiddenException;
import com.landslide.usermanagement.security.UserPrincipal;
import com.landslide.usermanagement.service.SubscriptionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

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

    @GetMapping("/{userId}/subscriptions")
    public ResponseEntity<List<SubscriptionResponse>> getSubscriptions(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserPrincipal caller) {

        // Admin can see any user's subscriptions; others only their own
        if (!caller.isAdmin() && !caller.getUserId().equals(userId)) {
            throw new ForbiddenException("Access denied");
        }
        return ResponseEntity.ok(subscriptionService.getSubscriptions(userId));
    }
}
