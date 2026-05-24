package com.landslide.usermanagement.mapper;

import com.landslide.usermanagement.model.UserProjectAlertSubscription;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SubscriptionMapper {

    List<UserProjectAlertSubscription> findByUserId(@Param("userId") Long userId);

    UserProjectAlertSubscription findByUserIdAndProjectId(
            @Param("userId") Long userId,
            @Param("projectId") String projectId
    );

    void insert(UserProjectAlertSubscription subscription);

    /** Returns true (1) if the project_id exists in the projects table */
    int projectExists(@Param("projectId") String projectId);
}
