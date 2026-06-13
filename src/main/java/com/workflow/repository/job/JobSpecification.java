package com.workflow.repository.job;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;
import com.workflow.entity.job.Job;
import com.workflow.common.constant.job.JobStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class JobSpecification {

    public static Specification<Job> buildAdvancedFilters(
            Long companyId,
            String search,
            String customerName,
            String clientName,
            String workflowName,
            String templateName,
            JobStatus status,
            Boolean archived,
            BigDecimal minNet,
            BigDecimal maxNet,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Mandatory Company Filter
            predicates.add(cb.equal(root.get("company").get("id"), companyId));

            // 2. Archive Filter (defaults to false if not provided)
            boolean isArchived = archived != null ? archived : false;
            predicates.add(cb.equal(root.get("archived"), isArchived));

            // 3. Exact Status Filter
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            // --- SAFE JOIN DECLARATIONS ---
            Join<Object, Object> customerJoin = root.join("customer", JoinType.LEFT);
            Join<Object, Object> clientJoin = root.join("client", JoinType.LEFT);
            Join<Object, Object> workflowJoin = root.join("workflow", JoinType.LEFT);
            Join<Object, Object> templateJoin = root.join("template", JoinType.LEFT);

            // 4. Specific Column Searches (Mini-CRM features)
            if (StringUtils.hasText(customerName)) {
                predicates.add(cb.like(cb.lower(customerJoin.get("name")),
                        "%" + customerName.toLowerCase() + "%"));
            }
            if (StringUtils.hasText(clientName)) {
                predicates.add(cb.like(cb.lower(clientJoin.get("name")),
                        "%" + clientName.toLowerCase() + "%"));
            }
            if (StringUtils.hasText(workflowName)) {
                predicates.add(cb.like(cb.lower(workflowJoin.get("name")),
                        "%" + workflowName.toLowerCase() + "%"));
            }
            if (StringUtils.hasText(templateName)) {
                predicates.add(cb.like(cb.lower(templateJoin.get("name")),
                        "%" + templateName.toLowerCase() + "%"));
            }

            // 5. Global Text Search (Upgraded to be truly global)
            if (StringUtils.hasText(search)) {
                String searchPattern = "%" + search.toLowerCase() + "%";

                Join<Object, Object> addressJoin = root.join("address", JoinType.LEFT);

                // Base Job Field
                Predicate matchJobRef = cb.like(cb.lower(root.get("jobRef").as(String.class)), searchPattern);

                // Related Entity Names
                Predicate matchCustomer = cb.like(cb.lower(customerJoin.get("name")), searchPattern);
                Predicate matchClient = cb.like(cb.lower(clientJoin.get("name")), searchPattern);
                Predicate matchWorkflow = cb.like(cb.lower(workflowJoin.get("name")), searchPattern);
                Predicate matchTemplate = cb.like(cb.lower(templateJoin.get("name")), searchPattern);

                // Expanded Address Search
                Predicate matchPostCode = cb.like(cb.lower(addressJoin.get("postalCode")), searchPattern);
                Predicate matchStreet = cb.like(cb.lower(addressJoin.get("street")), searchPattern);
                Predicate matchCity = cb.like(cb.lower(addressJoin.get("city")), searchPattern);
                Predicate matchState = cb.like(cb.lower(addressJoin.get("state")), searchPattern);

                predicates.add(cb.or(
                        matchJobRef,
                        matchCustomer,
                        matchClient,
                        matchWorkflow,
                        matchTemplate,
                        matchPostCode,
                        matchStreet,
                        matchCity,
                        matchState));
            }

            // 6. NUMBER RANGE FILTER (Estimate Total Net)
            if (minNet != null || maxNet != null) {
                Join<Object, Object> estimateJoin = root.join("estimate", JoinType.LEFT);

                if (minNet != null && maxNet != null) {
                    predicates.add(cb.between(estimateJoin.get("netAmount"), minNet, maxNet));
                } else if (minNet != null) {
                    predicates.add(cb.greaterThanOrEqualTo(estimateJoin.get("netAmount"), minNet));
                } else {
                    predicates.add(cb.lessThanOrEqualTo(estimateJoin.get("netAmount"), maxNet));
                }
            }

            // 7. DATE RANGE FILTER (Job Created At)
            if (startDate != null || endDate != null) {
                if (startDate != null && endDate != null) {
                    predicates.add(cb.between(root.get("createdAt"), startDate, endDate));
                } else if (startDate != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
                } else {
                    predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
                }
            }

            query.distinct(true);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}