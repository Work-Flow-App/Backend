package com.workflow.repository.notification;

import com.workflow.entity.notification.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId " +
            "AND (:cursor IS NULL OR n.id < :cursor) " +
            "ORDER BY n.id DESC")
    List<Notification> findByCursor(@Param("userId") Long userId,
            @Param("cursor") Long cursor,
            Pageable pageable);

    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId " +
            "AND n.isRead = :isRead " +
            "AND (:cursor IS NULL OR n.id < :cursor) " +
            "ORDER BY n.id DESC")
    List<Notification> findByCursorAndIsRead(@Param("userId") Long userId,
            @Param("isRead") boolean isRead,
            @Param("cursor") Long cursor,
            Pageable pageable);

    long countByUserIdAndIsReadFalse(Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP WHERE n.user.id = :userId AND n.isRead = false")
    void markAllAsReadByUserId(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM notifications WHERE is_read = true AND created_at < :cutoff ORDER BY id ASC LIMIT 1000", nativeQuery = true)
    int deleteReadNotificationsInBatches(@Param("cutoff") LocalDateTime cutoff);
}