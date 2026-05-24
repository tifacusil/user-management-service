package com.landslide.usermanagement.service;

import com.landslide.usermanagement.dto.UpdateUserRequest;
import com.landslide.usermanagement.dto.UserResponse;
import com.landslide.usermanagement.exception.ForbiddenException;
import com.landslide.usermanagement.exception.UserNotFoundException;
import com.landslide.usermanagement.mapper.UserMapper;
import com.landslide.usermanagement.model.User;
import com.landslide.usermanagement.security.UserPrincipal;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserMapper userMapper;

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public UserResponse getUser(Long userId, UserPrincipal caller) {
        checkAccess(userId, caller);
        User user = userMapper.findById(userId);
        if (user == null) throw new UserNotFoundException("User not found: " + userId);
        return UserResponse.from(user);
    }

    public UserResponse updateUser(Long userId, UpdateUserRequest req, UserPrincipal caller) {
        checkAccess(userId, caller);
        User user = userMapper.findById(userId);
        if (user == null) throw new UserNotFoundException("User not found: " + userId);

        userMapper.updateEmailAndPhone(userId, req.getEmail(), req.getPhoneE164());

        // Re-fetch to return updated state
        User updated = userMapper.findById(userId);
        return UserResponse.from(updated);
    }

    private void checkAccess(Long targetUserId, UserPrincipal caller) {
        if (!caller.isAdmin() && !caller.getUserId().equals(targetUserId)) {
            throw new ForbiddenException("Access denied: you can only access your own profile");
        }
    }
}
