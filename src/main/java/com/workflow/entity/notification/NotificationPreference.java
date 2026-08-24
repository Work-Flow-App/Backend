package com.workflow.entity.notification;

import com.workflow.common.constant.notification.NotificationType;
import com.workflow.entity.auth.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notification_preferences", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "user_id", "notification_type" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType notificationType;

    @Builder.Default
    @Column(name = "in_app_enabled")
    private boolean inAppEnabled = true;

    @Builder.Default
    @Column(name = "email_enabled")
    private boolean emailEnabled = true;

    @Builder.Default
    @Column(name = "push_enabled")
    private boolean pushEnabled = true;
}