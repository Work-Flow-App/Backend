package com.workflow.repository.worker;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.workflow.common.constant.worker.LeaveRequestStatus;
import com.workflow.entity.worker.WorkerLeaveRequest;

public interface WorkerLeaveRequestRepository extends JpaRepository<WorkerLeaveRequest, Long> {

    @Query("SELECT lr FROM WorkerLeaveRequest lr JOIN FETCH lr.worker LEFT JOIN FETCH lr.decisionBy WHERE lr.worker.id = :workerId ORDER BY lr.startDate DESC")
    List<WorkerLeaveRequest> findByWorkerIdOrderByStartDateDesc(@Param("workerId") Long workerId);

    @Query("SELECT lr FROM WorkerLeaveRequest lr JOIN FETCH lr.worker LEFT JOIN FETCH lr.decisionBy WHERE lr.id = :id AND lr.worker.id = :workerId")
    Optional<WorkerLeaveRequest> findByIdAndWorkerId(@Param("id") Long id, @Param("workerId") Long workerId);

    @Query("SELECT lr FROM WorkerLeaveRequest lr JOIN FETCH lr.worker w LEFT JOIN FETCH lr.decisionBy WHERE lr.id = :id AND w.company.id = :companyId")
    Optional<WorkerLeaveRequest> findByIdAndWorkerCompanyId(@Param("id") Long id, @Param("companyId") Long companyId);

    @Query("SELECT lr FROM WorkerLeaveRequest lr JOIN FETCH lr.worker w LEFT JOIN FETCH lr.decisionBy WHERE w.company.id = :companyId " +
            "AND (:status IS NULL OR lr.status = :status) ORDER BY lr.createdAt DESC")
    Page<WorkerLeaveRequest> findByCompanyId(@Param("companyId") Long companyId, @Param("status") LeaveRequestStatus status, Pageable pageable);

    @Query("SELECT lr FROM WorkerLeaveRequest lr WHERE lr.worker.id = :workerId AND lr.status IN ('PENDING','APPROVED') " +
            "AND lr.startDate <= :endDate AND lr.endDate >= :startDate AND (:excludeId IS NULL OR lr.id <> :excludeId)")
    List<WorkerLeaveRequest> findOverlapping(
            @Param("workerId") Long workerId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("excludeId") Long excludeId);

    @Query("SELECT lr FROM WorkerLeaveRequest lr JOIN FETCH lr.worker w WHERE w.company.id = :companyId " +
            "AND lr.status = 'APPROVED' AND lr.startDate <= :to AND lr.endDate >= :from ORDER BY lr.startDate ASC")
    List<WorkerLeaveRequest> findApprovedInRange(@Param("companyId") Long companyId, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
