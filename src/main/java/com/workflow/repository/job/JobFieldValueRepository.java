package com.workflow.repository.job;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import com.workflow.entity.job.JobFieldValue;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface JobFieldValueRepository extends JpaRepository<JobFieldValue, Long> {

    List<JobFieldValue> findByJobId(Long jobId);

    List<JobFieldValue> findByJobIdIn(List<Long> jobIds);

    Optional<JobFieldValue> findByJobIdAndFieldId(Long jobId, Long fieldId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("DELETE FROM JobFieldValue v WHERE v.job.id = :jobId")
    void deleteByJobId(Long jobId);
}
