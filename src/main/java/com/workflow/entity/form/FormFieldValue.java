package com.workflow.entity.form;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "form_field_values")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormFieldValue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    private FormSubmission submission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "field_id", nullable = false)
    private FormField field;

    @Column(columnDefinition = "TEXT")
    private String stringValue;

    private Boolean booleanValue;
    private LocalDateTime dateValue;

    @Column(columnDefinition = "JSON")
    private String jsonValue;

    // For file attachments
    private String fileUrl;
    private String fileName;
    private String fileType;
}