package com.landslide.usermanagement.integration;

import com.landslide.usermanagement.dto.DeviceRegisterRequest;
import com.landslide.usermanagement.dto.LoginRequest;
import com.landslide.usermanagement.dto.LoginResponse;
import com.landslide.usermanagement.mapper.DeviceMapper;
import com.landslide.usermanagement.mapper.UserMapper;
import com.landslide.usermanagement.model.User;
import com.landslide.usermanagement.model.UserDevice;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DeviceIntegrationTest {

    @Autowired private TestRestTemplate rest;
    @Autowired private UserMapper userMapper;
    @Autowired private DeviceMapper deviceMapper;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);

    // -------------------------------------------------------------------------
    // Test 8: POST /devices/register with token belonging to user A, called by
    //         user B → old record revoked, new record created for user B
    // -------------------------------------------------------------------------
    @Test
    void registerDevice_tokenOfOtherUser_revokesOldAndCreatesNew() {
        String suffix = String.valueOf(System.currentTimeMillis());

        // Create user A
        Long userAId = insertUser("device_userA_" + suffix, "device_usera_" + suffix + "@test.com");
        // Create user B
        Long userBId = insertUser("device_userB_" + suffix, "device_userb_" + suffix + "@test.com");

        String sharedToken = "fcm_test_token_" + suffix;

        // User A registers the token
        String tokenA = login("device_userA_" + suffix);
        registerDevice(sharedToken, "android", "1.0.0", tokenA);

        // Verify it belongs to user A
        UserDevice afterA = deviceMapper.findByToken(sharedToken);
        assertThat(afterA).isNotNull();
        assertThat(afterA.getUserId()).isEqualTo(userAId);
        assertThat(afterA.getStatus()).isEqualTo("active");

        // User B registers the SAME token
        String tokenB = login("device_userB_" + suffix);
        ResponseEntity<Map> response = registerDevice(sharedToken, "android", "1.0.1", tokenB);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // The token is now owned by user B (transferred in-place due to UNIQUE constraint)
        // User A should have NO active devices with that token
        var devicesA = deviceMapper.findActiveByUserId(userAId);
        boolean anyActiveWithToken = devicesA.stream()
                .anyMatch(d -> d.getDeviceToken().equals(sharedToken));
        assertThat(anyActiveWithToken).isFalse();

        // User B has the token as active
        var devicesB = deviceMapper.findActiveByUserId(userBId);
        boolean anyActiveForB = devicesB.stream()
                .anyMatch(d -> d.getDeviceToken().equals(sharedToken));
        assertThat(anyActiveForB).isTrue();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Long insertUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(encoder.encode("password123"));
        user.setRole("operator");
        user.setStatus("active");
        userMapper.insert(user);
        return user.getId();
    }

    private String login(String username) {
        LoginRequest req = new LoginRequest();
        req.setUsername(username);
        req.setPassword("password123");
        ResponseEntity<LoginResponse> resp =
                rest.postForEntity("/api/v1/auth/login", req, LoginResponse.class);
        return resp.getBody().getToken();
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<Map> registerDevice(String token, String platform,
                                                String version, String jwt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwt);
        DeviceRegisterRequest req = new DeviceRegisterRequest();
        req.setDeviceToken(token);
        req.setPlatform(platform);
        req.setAppVersion(version);
        return rest.postForEntity("/api/v1/devices/register",
                new HttpEntity<>(req, headers), Map.class);
    }
}
