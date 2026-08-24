package com.workflow.repository.notification;

import com.workflow.common.constant.notification.NotificationType;
import com.workflow.entity.notification.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {
    List<NotificationPreference> findByUserId(Long userId);

    Optional<NotificationPreference> findByUserIdAndNotificationType(Long userId, NotificationType type);
}