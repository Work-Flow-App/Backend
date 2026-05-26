package com.workflow.entity.financial;

import com.workflow.entity.company.Company;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "estimate_documents")
public class EstimateDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estimate_id", nullable = false)
    private Estimate estimate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "document_number", nullable = false, length = 50)
    private String documentNumber;

    @Column(name = "s3_key", nullable = false, length = 500)
    private String s3Key;

    @OneToMany(mappedBy = "estimateDocument", cascade = CascadeType.ALL, orphanRemoval = true,
               fetch = FetchType.LAZY)
    @Builder.Default
    private List<JobLineItemSnapshot> lineItemSnapshots = new ArrayList<>();

    @Column(name = "total_net", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalNet;

    @Column(name = "total_vat", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalVat;

    @Column(name = "grand_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal grandTotal;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "reference", length = 100)
    private String reference;

    @Column(name = "notes", length = 500)
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
