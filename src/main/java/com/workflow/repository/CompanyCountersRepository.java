package com.workflow.repository;

import com.workflow.entity.CompanyCounters;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CompanyCountersRepository extends JpaRepository<CompanyCounters, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CompanyCounters c WHERE c.companyId = :companyId")
    Optional<CompanyCounters> findByIdWithLock(Long companyId);
}
