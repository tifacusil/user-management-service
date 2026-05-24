package com.landslide.usermanagement.mapper;

import com.landslide.usermanagement.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {

    User findById(@Param("id") Long id);

    User findByUsername(@Param("username") String username);

    User findByEmail(@Param("email") String email);

    void insert(User user);

    void updateEmailAndPhone(
            @Param("id") Long id,
            @Param("email") String email,
            @Param("phoneE164") String phoneE164
    );

    /**
     * Returns all active users subscribed to the given project
     * whose min_alert_level is <= the given numeric level.
     * levelNum: blue=1, orange=2, red=3
     */
    List<User> findActiveRecipientsForProject(
            @Param("projectId") String projectId,
            @Param("levelNum") int levelNum
    );

    /** SELECT 1 — used by /health to verify DB connectivity */
    Integer healthCheck();
}
