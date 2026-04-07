package com.workflow.entity.job;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.workflow.common.constant.workflow.StepDiscussionType;
import com.workflow.entity.auth.User;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "job_workflow_step_attachments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobWorkflowStepAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_workflow_step_id", nullable = false)
    private JobWorkflowStep step;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StepDiscussionType type;

    @Column(columnDefinition = "TEXT")
    private String description; // 👈 text attached to file

    private String fileName;
    private String fileType;
    private String fileUrl;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}