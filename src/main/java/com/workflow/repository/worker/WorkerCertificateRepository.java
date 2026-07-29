package com.workflow.repository.worker;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.workflow.entity.worker.WorkerCertificate;

public interface WorkerCertificateRepository extends JpaRepository<WorkerCertificate, Long> {

    @Query("SELECT c FROM WorkerCertificate c JOIN FETCH c.worker JOIN FETCH c.uploadedBy WHERE c.worker.id = :workerId ORDER BY c.expiryDate ASC")
    List<WorkerCertificate> findByWorkerId(@Param("workerId") Long workerId);

    @Query("SELECT c FROM WorkerCertificate c JOIN FETCH c.worker JOIN FETCH c.uploadedBy WHERE c.id = :id AND c.worker.id = :workerId")
    Optional<WorkerCertificate> findByIdAndWorkerId(@Param("id") Long id, @Param("workerId") Long workerId);

    @Query("SELECT c FROM WorkerCertificate c JOIN FETCH c.worker w JOIN FETCH c.uploadedBy WHERE c.id = :id AND w.id = :workerId AND w.company.id = :companyId")
    Optional<WorkerCertificate> findByIdAndWorkerIdAndCompanyId(
            @Param("id") Long id, @Param("workerId") Long workerId, @Param("companyId") Long companyId);

    @Query("SELECT c FROM WorkerCertificate c JOIN FETCH c.worker w JOIN FETCH c.uploadedBy WHERE w.id = :workerId AND w.company.id = :companyId ORDER BY c.expiryDate ASC")
    List<WorkerCertificate> findByWorkerIdAndCompanyId(@Param("workerId") Long workerId, @Param("companyId") Long companyId);

    @Query("SELECT c FROM WorkerCertificate c JOIN FETCH c.worker w JOIN FETCH c.uploadedBy WHERE w.company.id = :companyId AND w.archived = false")
    Page<WorkerCertificate> findByCompanyId(@Param("companyId") Long companyId, Pageable pageable);

    @Query("SELECT c FROM WorkerCertificate c JOIN FETCH c.worker w WHERE w.company.id = :companyId AND w.archived = false " +
            "AND c.expiryDate IS NOT NULL AND c.expiryDate <= :threshold ORDER BY c.expiryDate ASC")
    List<WorkerCertificate> findExpiringByCompanyId(@Param("companyId") Long companyId, @Param("threshold") LocalDate threshold);
}
