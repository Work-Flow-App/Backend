package com.workflow.entity.worker;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.workflow.common.constant.worker.CertificateType;
import com.workflow.entity.auth.User;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "worker_certificates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkerCertificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CertificateType type;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "issuing_authority", length = 150)
    private String issuingAuthority;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_type")
    private String fileType;

    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
