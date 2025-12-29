package com.workflow.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "workflow_steps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    private Workflow workflow;

    @Column(nullable = false, length = 150)
    private String name;

    private String description;

    @Column(nullable = false)
    private Integer orderIndex;

    @Builder.Default
    private boolean optional = false;
}
