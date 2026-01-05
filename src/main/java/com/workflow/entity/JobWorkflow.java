package com.workflow.entity;

import com.workflow.common.constant.workflow.WorkflowStepStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "job_workflows")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobWorkflow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    private Workflow workflow;

    @CreationTimestamp
    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    @Enumerated(EnumType.STRING)
    private WorkflowStepStatus status;

    @ManyToMany
    @JoinTable(name = "job_workflow_workers", joinColumns = @JoinColumn(name = "job_workflow_id"), inverseJoinColumns = @JoinColumn(name = "worker_id"))
    @Builder.Default
    private Set<Worker> workers = new HashSet<>();
}
