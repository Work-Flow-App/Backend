package com.workflow.entity.job;

import com.workflow.common.constant.job.JobFieldType;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "job_template_fields")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class JobTemplateField {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private JobTemplate template;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 150)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobFieldType jobFieldType;

    @Column(nullable = false)
    @Builder.Default
    private boolean required = false;

    @Column(columnDefinition = "TEXT")
    private String options;     // JSON array for dropdown

    @Column(name = "order_index")
    private Integer orderIndex;
}
