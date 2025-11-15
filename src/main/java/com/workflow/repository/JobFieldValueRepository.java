package com.workflow.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import com.workflow.entity.JobFieldValue;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JobFieldValueRepository extends JpaRepository<JobFieldValue, Long> {

    List<JobFieldValue> findByJobId(Long jobId);

    @Modifying
    @Transactional
    @Query("DELETE FROM JobFieldValue v WHERE v.job.id = :jobId")
    void deleteByJobId(Long jobId);
}
