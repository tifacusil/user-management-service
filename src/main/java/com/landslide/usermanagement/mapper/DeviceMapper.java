package com.landslide.usermanagement.mapper;

import com.landslide.usermanagement.model.UserDevice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DeviceMapper {

    UserDevice findByToken(@Param("token") String token);

    List<UserDevice> findActiveByUserId(@Param("userId") Long userId);

    void insert(UserDevice device);

    void updateLastSeen(@Param("token") String token);

    void revokeByToken(@Param("token") String token);

    /**
     * Transfers a device token to a new user (called when the same token
     * was previously registered by a different user).
     * Updates user_id, platform, app_version, last_seen_at and status=active.
     */
    void transferToUser(
            @Param("token") String token,
            @Param("userId") Long userId,
            @Param("platform") String platform,
            @Param("appVersion") String appVersion
    );
}
