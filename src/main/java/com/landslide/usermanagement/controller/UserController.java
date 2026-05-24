package com.landslide.usermanagement.controller;

import com.landslide.usermanagement.dto.UpdateUserRequest;
import com.landslide.usermanagement.dto.UserResponse;
import com.landslide.usermanagement.security.UserPrincipal;
import com.landslide.usermanagement.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserPrincipal caller) {
        return ResponseEntity.ok(userService.getUser(userId, caller));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long userId,
            @RequestBody UpdateUserRequest req,
            @AuthenticationPrincipal UserPrincipal caller) {
        return ResponseEntity.ok(userService.updateUser(userId, req, caller));
    }
}
