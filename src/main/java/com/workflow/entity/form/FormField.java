package com.workflow.entity.form;

import com.workflow.common.constant.form.FormFieldType;
import com.workflow.common.constant.form.FormRoleTarget;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "form_fields")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormField {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private FormTemplate template;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 150)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FormFieldType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private FormRoleTarget roleTarget = FormRoleTarget.BOTH;

    @Column(nullable = false)
    @Builder.Default
    private boolean required = false;

    @Column(columnDefinition = "TEXT")
    private String options; // JSON array for DROPDOWN/MULTI_SELECT

    @Column(name = "order_index")
    private Integer orderIndex;
}