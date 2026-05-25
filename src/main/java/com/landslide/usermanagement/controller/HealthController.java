package com.landslide.usermanagement.controller;

import com.landslide.usermanagement.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@Tag(name = "Health", description = "Liveness probe polled by Consul every 10 seconds")
@RestController
public class HealthController {

    private final UserMapper userMapper;

    public HealthController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Operation(
            summary = "Health check",
            description = "No authentication required. Returns `200` when MySQL is reachable, `503` otherwise.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Service and DB are UP",
                            content = @Content(examples = @ExampleObject(value = """
                                    {"service": "user-management", "status": "UP", "db": "UP"}"""))),
                    @ApiResponse(responseCode = "503", description = "MySQL is unreachable",
                            content = @Content(examples = @ExampleObject(value = """
                                    {"service": "user-management", "status": "DOWN", "db": "DOWN"}""")))
            }
    )
    @SecurityRequirements
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("service", "user-management");

        try {
            userMapper.healthCheck();
            body.put("status", "UP");
            body.put("db", "UP");
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            body.put("status", "DOWN");
            body.put("db", "DOWN");
            return ResponseEntity.status(503).body(body);
        }
    }
}
