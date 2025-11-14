package com.workflow.repository;

import com.workflow.entity.JobFieldValue;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JobFieldValueRepository extends JpaRepository<JobFieldValue, Long> {
    List<JobFieldValue> findByJobId(Long jobId);
    void deleteByJobId(Long jobId);
}
