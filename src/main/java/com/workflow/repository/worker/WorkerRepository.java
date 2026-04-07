package com.workflow.repository.worker;

import com.workflow.entity.worker.Worker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WorkerRepository extends JpaRepository<Worker, Long> {

    @Query("SELECT w FROM Worker w WHERE w.company.id = :companyId AND w.archived = false")
    List<Worker> findByCompanyIdAndNotArchived(@Param("companyId") Long companyId);

    @Query("SELECT w FROM Worker w WHERE w.id = :workerId AND w.company.id = :companyId AND w.archived = false")
    Optional<Worker> findByIdAndCompanyIdAndNotArchived(@Param("workerId") Long workerId, @Param("companyId") Long companyId);

    @Query("SELECT w FROM Worker w WHERE w.user.id = :userId")
    Optional<Worker> findByUserId(@Param("userId") Long userId);

    boolean existsByUserIdAndArchivedFalse(Long userId);

    boolean existsByEmailIgnoreCaseAndArchivedFalse(String email);
}
