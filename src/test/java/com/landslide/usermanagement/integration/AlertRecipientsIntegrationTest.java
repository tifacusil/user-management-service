package com.landslide.usermanagement.integration;

import com.landslide.usermanagement.config.AppConfig;
import com.landslide.usermanagement.dto.AlertRecipientsResponse;
import com.landslide.usermanagement.dto.LoginRequest;
import com.landslide.usermanagement.dto.LoginResponse;
import com.landslide.usermanagement.dto.RegisterRequest;
import com.landslide.usermanagement.dto.SubscriptionRequest;
import com.landslide.usermanagement.mapper.DeviceMapper;
import com.landslide.usermanagement.mapper.SubscriptionMapper;
import com.landslide.usermanagement.mapper.UserMapper;
import com.landslide.usermanagement.model.User;
import com.landslide.usermanagement.model.UserDevice;
import com.landslide.usermanagement.model.UserProjectAlertSubscription;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.main.allow-bean-definition-overriding=true"
)
class AlertRecipientsIntegrationTest {

    /**
     * Provides a fixed clock at 23:00 UTC for quiet-hours tests (6 & 7).
     * Overrides the production UTC clock.
     */
    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        public Clock clock() {
            // Fixed at 23:00 UTC today
            Instant fixedAt23 = LocalDate.now(ZoneOffset.UTC)
                    .atTime(23, 0)
                    .toInstant(ZoneOffset.UTC);
            return Clock.fixed(fixedAt23, ZoneOffset.UTC);
        }
    }

    @Autowired private TestRestTemplate rest;
    @Autowired private UserMapper userMapper;
    @Autowired private SubscriptionMapper subscriptionMapper;
    @Autowired private DeviceMapper deviceMapper;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);

    // Test project
    private static final String PROJECT_ID = "shangtan";

    // Users created by this test
    private final List<Long> createdUserIds = new ArrayList<>();

    private Long adminId;
    private Long operadorId;
    private Long autoridadId;
    private String adminToken;

    @BeforeEach
    void setupTestData() {
        String suffix = String.valueOf(System.currentTimeMillis());

        // Create admin user
        adminId = createUser("test_admin_" + suffix,
                "test_admin_" + suffix + "@test.com", "password123", "admin",
                "+51999111001");

        // Create operador_shangtan: subscribed at blue level with quiet_hours 22:00-06:00
        operadorId = createUser("test_operador_" + suffix,
                "test_operador_" + suffix + "@test.com", "password123", "operator",
                "+51999111002");
        createSubscription(operadorId, PROJECT_ID, "blue",
                true, true, true, "22:00", "06:00");

        // Create autoridad_regional: subscribed at red level only, no quiet hours
        autoridadId = createUser("test_autoridad_" + suffix,
                "test_autoridad_" + suffix + "@test.com", "password123", "viewer",
                "+51999111003");
        createSubscription(autoridadId, PROJECT_ID, "red",
                true, false, true, null, null);

        // Create subscription for admin at blue level (always included)
        createSubscription(adminId, PROJECT_ID, "blue",
                true, true, true, null, null);

        // Get admin JWT for authorized requests
        adminToken = loginAndGetToken("test_admin_" + suffix, "password123");
    }

    @AfterEach
    void cleanup() {
        // NOTE: In production CI, use a test-only database that is reset between runs.
        // These test users will remain in the shared DB. Use unique usernames (with timestamp)
        // to avoid conflicts between test runs.
    }

    // -------------------------------------------------------------------------
    // Test 4: level=orange → includes admin & operador, NOT autoridad
    // -------------------------------------------------------------------------
    @Test
    void alertRecipients_orange_includesBlueAndOrange_excludesRedOnly() {
        AlertRecipientsResponse body = getRecipients(PROJECT_ID, "orange");

        assertThat(body).isNotNull();
        assertThat(body.getAlertLevel()).isEqualTo("orange");

        List<Long> recipientIds = body.getRecipients().stream()
                .map(AlertRecipientsResponse.RecipientDto::getUserId)
                .toList();

        assertThat(recipientIds).contains(adminId, operadorId);
        assertThat(recipientIds).doesNotContain(autoridadId);
    }

    // -------------------------------------------------------------------------
    // Test 5: level=red → includes all (autoridad has min_alert_level=red)
    // -------------------------------------------------------------------------
    @Test
    void alertRecipients_red_includesAllSubscribers() {
        AlertRecipientsResponse body = getRecipients(PROJECT_ID, "red");

        List<Long> recipientIds = body.getRecipients().stream()
                .map(AlertRecipientsResponse.RecipientDto::getUserId)
                .toList();

        assertThat(recipientIds).contains(adminId, operadorId, autoridadId);
    }

    // -------------------------------------------------------------------------
    // Test 6: level=orange at 23:00 UTC → operador has suppressed_by_quiet_hours=true
    //         (Clock is fixed at 23:00 UTC by FixedClockConfig)
    // -------------------------------------------------------------------------
    @Test
    void alertRecipients_orange_at23Utc_operadorSuppressed() {
        AlertRecipientsResponse body = getRecipients(PROJECT_ID, "orange");

        AlertRecipientsResponse.RecipientDto operador = body.getRecipients().stream()
                .filter(r -> r.getUserId().equals(operadorId))
                .findFirst()
                .orElse(null);

        assertThat(operador).isNotNull();
        assertThat(operador.getChannels().getSms().isSuppressedByQuietHours()).isTrue();
    }

    // -------------------------------------------------------------------------
    // Test 7: level=red at 23:00 UTC → operador has suppressed_by_quiet_hours=false
    //         (Red alerts are NEVER suppressed, even in quiet hours)
    // -------------------------------------------------------------------------
    @Test
    void alertRecipients_red_at23Utc_operadorNotSuppressed() {
        AlertRecipientsResponse body = getRecipients(PROJECT_ID, "red");

        AlertRecipientsResponse.RecipientDto operador = body.getRecipients().stream()
                .filter(r -> r.getUserId().equals(operadorId))
                .findFirst()
                .orElse(null);

        assertThat(operador).isNotNull();
        assertThat(operador.getChannels().getSms().isSuppressedByQuietHours()).isFalse();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private AlertRecipientsResponse getRecipients(String projectId, String level) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<AlertRecipientsResponse> response = rest.exchange(
                "/api/v1/projects/" + projectId + "/alert-recipients?level=" + level,
                HttpMethod.GET, entity, AlertRecipientsResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private Long createUser(String username, String email, String password,
                            String role, String phone) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(encoder.encode(password));
        user.setRole(role);
        user.setStatus("active");
        user.setPhoneE164(phone);
        userMapper.insert(user);
        createdUserIds.add(user.getId());
        return user.getId();
    }

    private void createSubscription(Long userId, String projectId, String minLevel,
                                    boolean sms, boolean push, boolean email,
                                    String qStart, String qEnd) {
        UserProjectAlertSubscription sub = new UserProjectAlertSubscription();
        sub.setUserId(userId);
        sub.setProjectId(projectId);
        sub.setMinAlertLevel(minLevel);
        sub.setSmsEnabled(sms);
        sub.setPushEnabled(push);
        sub.setEmailEnabled(email);
        sub.setQuietHoursStart(qStart);
        sub.setQuietHoursEnd(qEnd);
        subscriptionMapper.insert(sub);
    }

    private String loginAndGetToken(String username, String password) {
        LoginRequest req = new LoginRequest();
        req.setUsername(username);
        req.setPassword(password);
        ResponseEntity<LoginResponse> resp =
                rest.postForEntity("/api/v1/auth/login", req, LoginResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return resp.getBody().getToken();
    }
}
