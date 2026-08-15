package com.workflow.repository.notification;

import com.workflow.entity.notification.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {
    List<DeviceToken> findByUserIdAndIsActiveTrue(Long userId);

    void deleteByPushToken(String pushToken);
}