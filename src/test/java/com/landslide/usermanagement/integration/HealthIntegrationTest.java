package com.landslide.usermanagement.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HealthIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    // -------------------------------------------------------------------------
    // Test 9: GET /health → 200 when MySQL responds
    // -------------------------------------------------------------------------
    @Test
    @SuppressWarnings("unchecked")
    void health_returns200_whenDbIsUp() {
        ResponseEntity<Map> response = rest.getForEntity("/health", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("UP");
        assertThat(response.getBody().get("db")).isEqualTo("UP");
        assertThat(response.getBody().get("service")).isEqualTo("user-management");
    }
}
