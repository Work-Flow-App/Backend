package com.workflow.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "workflows", uniqueConstraints = @UniqueConstraint(columnNames = { "company_id", "name" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Workflow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false, length = 150)
    private String name;

    private String description;

    @Builder.Default
    @Column(name = "workflow_ref", nullable = false)
    private Long workflowRef = 0L;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
