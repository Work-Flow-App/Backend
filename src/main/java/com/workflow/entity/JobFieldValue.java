package com.workflow.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "job_field_values")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class JobFieldValue {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "field_id", nullable = false)
    private JobTemplateField field;

    @Column(columnDefinition = "TEXT")
    private String value;
}
