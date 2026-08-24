package com.workflow.repository.worker;

import com.workflow.entity.worker.Worker;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WorkerRepository extends JpaRepository<Worker, Long> {

    @Query("SELECT w FROM Worker w WHERE w.company.id = :companyId AND w.archived = false")
    List<Worker> findByCompanyIdAndNotArchived(@Param("companyId") Long companyId);

    /**
     * Locks the worker row for the duration of the transaction. Used to serialize
     * concurrent leave-request submissions for the same worker so the overlap
     * check + insert is not a check-then-act race.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Worker w WHERE w.id = :id")
    Optional<Worker> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT w FROM Worker w WHERE w.id = :workerId AND w.company.id = :companyId AND w.archived = false")
    Optional<Worker> findByIdAndCompanyIdAndNotArchived(@Param("workerId") Long workerId, @Param("companyId") Long companyId);

    @Query("SELECT w FROM Worker w WHERE w.user.id = :userId")
    Optional<Worker> findByUserId(@Param("userId") Long userId);

    @Query("SELECT w FROM Worker w WHERE w.user.id = :userId AND w.archived = false")
    Optional<Worker> findByUserIdAndArchivedFalse(@Param("userId") Long userId);

    boolean existsByUserIdAndArchivedFalse(Long userId);

    boolean existsByEmailIgnoreCaseAndArchivedFalse(String email);

    @Query("SELECT w FROM Worker w JOIN FETCH w.user WHERE w.company.id = :companyId AND w.user.id = :userId AND w.archived = true")
    Optional<Worker> findArchivedByCompanyIdAndUserId(@Param("companyId") Long companyId, @Param("userId") Long userId);

    long countByCompanyIdAndArchivedFalse(Long companyId);
}
