package com.workflow.repository.job;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.workflow.entity.JobFieldValue;

@Repository
public interface JobFieldValueRepository extends JpaRepository<JobFieldValue, Long> {
    List<JobFieldValue> findByJobId(Long jobId);
}

