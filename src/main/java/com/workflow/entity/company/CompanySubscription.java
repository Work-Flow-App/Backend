package com.workflow.entity.company;

import com.workflow.common.constant.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "company_subscriptions")
public class CompanySubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false, unique = true)
    private Company company;

    @Column(name = "trial_ends_at", nullable = false)
    private LocalDateTime trialEndsAt;

    @Column(name = "paddle_customer_id", length = 100)
    private String paddleCustomerId;

    @Column(name = "paddle_subscription_id", length = 100)
    private String paddleSubscriptionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SubscriptionStatus status = SubscriptionStatus.TRIAL;

    @Column(name = "current_period_end")
    private LocalDateTime currentPeriodEnd;

    @Column(name = "last_event_occurred_at")
    private LocalDateTime lastEventOccurredAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", insertable = false)
    private LocalDateTime updatedAt;

    /**
     * Returns true when the company is allowed to access the application.
     * pastDueGraceDays: number of days after currentPeriodEnd before PAST_DUE is blocked.
     */
    public boolean isAccessAllowed(int pastDueGraceDays) {
        LocalDateTime now = LocalDateTime.now();
        return switch (status) {
            case TRIAL -> now.isBefore(trialEndsAt);
            case ACTIVE -> true;
            case PAST_DUE -> currentPeriodEnd != null
                    && now.isBefore(currentPeriodEnd.plusDays(pastDueGraceDays));
            case PAUSED, CANCELLED, EXPIRED -> false;
        };
    }

    /**
     * Returns true if the incoming Paddle event is newer than the last processed event.
     * Prevents out-of-order webhook replays from rolling back state.
     */
    public boolean isNewerEvent(LocalDateTime eventOccurredAt) {
        return lastEventOccurredAt == null || eventOccurredAt.isAfter(lastEventOccurredAt);
    }
}