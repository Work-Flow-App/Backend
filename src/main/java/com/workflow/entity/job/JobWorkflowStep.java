package com.workflow.entity.job;

import com.workflow.common.constant.workflow.WorkflowStepStatus;
import com.workflow.entity.worker.Worker;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "job_workflow_steps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobWorkflowStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_workflow_id", nullable = false)
    private JobWorkflow jobWorkflow;

    @Column(nullable = false, length = 255)
    private String name;
    private String description;

    @Column(nullable = false)
    private Integer orderIndex;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkflowStepStatus status;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "job_workflow_step_workers", joinColumns = @JoinColumn(name = "job_workflow_step_id"), inverseJoinColumns = @JoinColumn(name = "worker_id"))
    @Builder.Default
    private Set<Worker> assignedWorkers = new HashSet<>();
}