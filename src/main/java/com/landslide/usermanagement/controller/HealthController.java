package com.landslide.usermanagement.controller;

import com.landslide.usermanagement.mapper.UserMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {

    private final UserMapper userMapper;

    public HealthController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

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
