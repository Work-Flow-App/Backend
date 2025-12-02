package com.workflow.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "job_field_values")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobFieldValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "field_id", nullable = false)
    private JobTemplateField field;

    // Primitive types
    @Column(columnDefinition = "TEXT")
    private String stringValue;

    private Double numberValue;

    private Boolean booleanValue;

    private LocalDateTime dateValue;

    // JSON object (any structure)
    @Column(columnDefinition = "JSON")
    private String jsonValue;

    // Object references / foreign keys
    private Long referenceId;

    @Column(length = 150)
    private String referenceType; // e.g., "Client", "Worker", "CustomObject"

    // Convenience method to get actual value based on field type
    public Object getTypedValue() {
        if (field == null || field.getJobFieldType() == null)
            return null;

        return switch (field.getJobFieldType()) {
            case TEXT, DROPDOWN -> stringValue;
            case NUMBER -> numberValue;
            case BOOLEAN -> booleanValue;
            case DATE -> dateValue;
            case JSON -> jsonValue;
            case REFERENCE -> referenceId;
        };
    }
}
