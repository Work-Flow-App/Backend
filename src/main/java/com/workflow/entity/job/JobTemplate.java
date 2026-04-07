package com.workflow.entity.job;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.workflow.entity.company.Company;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "job_templates")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class JobTemplate {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean isDefault;

    @Builder.Default
    @Column(name = "template_ref", nullable = false)
    private Long templateRef = 0L;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
