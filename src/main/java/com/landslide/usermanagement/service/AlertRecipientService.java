package com.landslide.usermanagement.service;

import com.landslide.usermanagement.dto.AlertRecipientsResponse;
import com.landslide.usermanagement.dto.AlertRecipientsResponse.*;
import com.landslide.usermanagement.mapper.DeviceMapper;
import com.landslide.usermanagement.mapper.SubscriptionMapper;
import com.landslide.usermanagement.mapper.UserMapper;
import com.landslide.usermanagement.model.User;
import com.landslide.usermanagement.model.UserDevice;
import com.landslide.usermanagement.model.UserProjectAlertSubscription;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AlertRecipientService {

    private final UserMapper userMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final DeviceMapper deviceMapper;
    private final Clock clock;

    public AlertRecipientService(UserMapper userMapper,
                                 SubscriptionMapper subscriptionMapper,
                                 DeviceMapper deviceMapper,
                                 Clock clock) {
        this.userMapper = userMapper;
        this.subscriptionMapper = subscriptionMapper;
        this.deviceMapper = deviceMapper;
        this.clock = clock;
    }

    public AlertRecipientsResponse getRecipients(String projectId, String level) {
        int levelNum = levelToNum(level);

        List<User> users = userMapper.findActiveRecipientsForProject(projectId, levelNum);

        List<RecipientDto> recipients = users.stream()
                .map(user -> buildRecipient(user, projectId, level))
                .collect(Collectors.toList());

        String generatedAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now(clock));
        return new AlertRecipientsResponse(projectId, level, recipients, generatedAt);
    }

    private RecipientDto buildRecipient(User user, String projectId, String level) {
        UserProjectAlertSubscription sub =
                subscriptionMapper.findByUserIdAndProjectId(user.getId(), projectId);

        List<UserDevice> devices = deviceMapper.findActiveByUserId(user.getId());

        boolean inQuietHours = isInQuietHours(sub);
        // Red alerts are NEVER suppressed by quiet hours
        boolean suppressed = inQuietHours && !"red".equalsIgnoreCase(level);

        // SMS channel
        SmsChannelDto sms = new SmsChannelDto(
                sub.isSmsEnabled(),
                user.getPhoneE164(),
                suppressed
        );

        // Push channel
        List<DeviceDto> deviceDtos = devices.stream()
                .map(d -> new DeviceDto(d.getDeviceToken(), d.getPlatform()))
                .collect(Collectors.toList());
        PushChannelDto push = new PushChannelDto(sub.isPushEnabled(), deviceDtos);

        // Email channel
        EmailChannelDto email = new EmailChannelDto(sub.isEmailEnabled(), user.getEmail());

        ChannelsDto channels = new ChannelsDto(sms, push, email);
        return new RecipientDto(user.getId(), user.getUsername(), user.getRole(), channels);
    }

    /**
     * Checks if the current UTC time falls within the quiet hours window.
     * Handles midnight crossing (e.g. 22:00 – 06:00).
     */
    private boolean isInQuietHours(UserProjectAlertSubscription sub) {
        String startStr = sub.getQuietHoursStart();
        String endStr = sub.getQuietHoursEnd();
        if (startStr == null || endStr == null) return false;

        // Normalize HH:MM:SS → HH:MM
        if (startStr.length() > 5) startStr = startStr.substring(0, 5);
        if (endStr.length() > 5)   endStr   = endStr.substring(0, 5);

        LocalTime now   = LocalTime.now(clock.withZone(ZoneOffset.UTC));
        LocalTime start = LocalTime.parse(startStr);
        LocalTime end   = LocalTime.parse(endStr);

        if (start.isAfter(end)) {
            // Crosses midnight: e.g. 22:00 – 06:00
            // In range if: time >= 22:00  OR  time < 06:00
            return !now.isBefore(start) || now.isBefore(end);
        } else {
            // Normal range: e.g. 08:00 – 20:00
            return !now.isBefore(start) && now.isBefore(end);
        }
    }

    private int levelToNum(String level) {
        return switch (level.toLowerCase()) {
            case "blue"   -> 1;
            case "orange" -> 2;
            case "red"    -> 3;
            default -> throw new IllegalArgumentException("Unknown alert level: " + level);
        };
    }
}
