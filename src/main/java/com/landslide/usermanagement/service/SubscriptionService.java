package com.landslide.usermanagement.service;

import com.landslide.usermanagement.dto.SubscriptionRequest;
import com.landslide.usermanagement.dto.SubscriptionResponse;
import com.landslide.usermanagement.exception.DuplicateSubscriptionException;
import com.landslide.usermanagement.exception.ProjectNotFoundException;
import com.landslide.usermanagement.exception.UserNotFoundException;
import com.landslide.usermanagement.mapper.SubscriptionMapper;
import com.landslide.usermanagement.mapper.UserMapper;
import com.landslide.usermanagement.model.User;
import com.landslide.usermanagement.model.UserProjectAlertSubscription;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SubscriptionService {

    private final SubscriptionMapper subscriptionMapper;
    private final UserMapper userMapper;

    public SubscriptionService(SubscriptionMapper subscriptionMapper, UserMapper userMapper) {
        this.subscriptionMapper = subscriptionMapper;
        this.userMapper = userMapper;
    }

    public SubscriptionResponse createSubscription(Long userId, SubscriptionRequest req) {
        // Validate user exists
        User user = userMapper.findById(userId);
        if (user == null) throw new UserNotFoundException("User not found: " + userId);

        // Validate project exists
        if (subscriptionMapper.projectExists(req.getProjectId()) == 0) {
            throw new ProjectNotFoundException("Project not found: " + req.getProjectId());
        }

        // Check for duplicate
        if (subscriptionMapper.findByUserIdAndProjectId(userId, req.getProjectId()) != null) {
            throw new DuplicateSubscriptionException(
                    "Subscription already exists for user " + userId
                            + " and project " + req.getProjectId());
        }

        UserProjectAlertSubscription sub = new UserProjectAlertSubscription();
        sub.setUserId(userId);
        sub.setProjectId(req.getProjectId());
        sub.setMinAlertLevel(req.getMinAlertLevel());
        sub.setSmsEnabled(req.isSmsEnabled());
        sub.setPushEnabled(req.isPushEnabled());
        sub.setEmailEnabled(req.isEmailEnabled());
        sub.setQuietHoursStart(req.getQuietHoursStart());
        sub.setQuietHoursEnd(req.getQuietHoursEnd());

        subscriptionMapper.insert(sub);

        return SubscriptionResponse.from(sub);
    }

    public List<SubscriptionResponse> getSubscriptions(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) throw new UserNotFoundException("User not found: " + userId);

        return subscriptionMapper.findByUserId(userId).stream()
                .map(SubscriptionResponse::from)
                .collect(Collectors.toList());
    }
}
