package com.workflow.entity.notification;

import com.workflow.entity.auth.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "device_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String platform; // e.g., "IOS", "ANDROID", "WEB"

    // MySQL cannot index TEXT for unique constraints. 
    // VARCHAR(512) is safe and easily covers FCM/APNs token lengths.
    @Column(name = "push_token", nullable = false, unique = true, length = 512)
    private String pushToken;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @CreationTimestamp
    private LocalDateTime createdAt;
}