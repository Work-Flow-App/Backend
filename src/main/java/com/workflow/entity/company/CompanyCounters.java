package com.workflow.entity.company;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "company_counters")
public class CompanyCounters {

    @Id
    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "next_job_id", nullable = false)
    private Long nextJobId;

    @Column(name = "next_worker_id", nullable = false)
    private Long nextWorkerId;

    @Column(name = "next_customer_id", nullable = false)
    private Long nextCustomerId;

    @Column(name = "next_client_id", nullable = false)
    private Long nextClientId;

    @Column(name = "next_asset_id", nullable = false)
    private Long nextAssetId;

    @Column(name = "next_template_id", nullable = false)
    private Long nextTemplateId;

    @Column(name = "next_workflow_id", nullable = false)
    private Long nextWorkflowId;

    @Column(name = "next_invoice_id", nullable = false)
    @Builder.Default
    private Long nextInvoiceId = 1L;
}
