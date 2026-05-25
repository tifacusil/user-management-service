package com.landslide.usermanagement.controller;

import com.landslide.usermanagement.dto.UpdateUserRequest;
import com.landslide.usermanagement.dto.UserResponse;
import com.landslide.usermanagement.security.UserPrincipal;
import com.landslide.usermanagement.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Users", description = "User profile management")
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(
            summary = "Get a user profile",
            description = "Admins can fetch any user. Operators and viewers can only fetch themselves.",
            parameters = @Parameter(name = "userId", description = "Target user ID", required = true, example = "1"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "User profile returned",
                            content = @Content(schema = @Schema(implementation = UserResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Accessing another user without admin role",
                            content = @Content(examples = @ExampleObject(value = """
                                    {"error": "Forbidden"}"""))),
                    @ApiResponse(responseCode = "404", description = "User not found",
                            content = @Content(examples = @ExampleObject(value = """
                                    {"error": "User not found"}""")))
            }
    )
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserPrincipal caller) {
        return ResponseEntity.ok(userService.getUser(userId, caller));
    }

    @Operation(
            summary = "Update a user profile",
            description = "Updates `email` and/or `phone_e164`. The fields `username`, `role`, and `status` cannot be changed via this endpoint.",
            parameters = @Parameter(name = "userId", description = "Target user ID", required = true, example = "1"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Updated user profile",
                            content = @Content(schema = @Schema(implementation = UserResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Updating another user without admin role"),
                    @ApiResponse(responseCode = "404", description = "User not found")
            }
    )
    @PutMapping("/{userId}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long userId,
            @RequestBody UpdateUserRequest req,
            @AuthenticationPrincipal UserPrincipal caller) {
        return ResponseEntity.ok(userService.updateUser(userId, req, caller));
    }
}
