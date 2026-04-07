package com.workflow.entity.asset;

import com.workflow.entity.job.Job;
import com.workflow.entity.worker.Worker;
import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "asset_job_assignments", indexes = {
    @Index(name = "idx_assignment_asset", columnList = "asset_id"),
    @Index(name = "idx_assignment_job", columnList = "job_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetJobAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Which asset
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    // Which job (nullable if assigned to person / non-job)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    private Job job;

    // Optionally assign to a Worker (when giving to a person)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_worker_id")
    private Worker assignedWorker;

    @Column(length = 500)
    private String notes;

    @Column(nullable = false)
    private LocalDateTime assignedAt;

    private LocalDateTime returnedAt;

    // read-only convenience
    public boolean isActive() {
        return returnedAt == null;
    }
}
