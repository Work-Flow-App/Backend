package com.workflow.repository;

import com.workflow.entity.WorkerInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorkerInvitationRepository extends JpaRepository<WorkerInvitation, Long> {

    /**
     * Find invitation by token
     */
    Optional<WorkerInvitation> findByInvitationToken(String invitationToken);

    /**
     * Find active invitation by email and company
     */
    @Query("SELECT i FROM WorkerInvitation i WHERE i.email = :email AND i.company.id = :companyId AND i.used = false AND i.expiresAt > :now")
    Optional<WorkerInvitation> findActiveInvitationByEmailAndCompany(
            @Param("email") String email,
            @Param("companyId") Long companyId,
            @Param("now") LocalDateTime now
    );

    /**
     * Invalidate all previous invitations for an email/company combination
     */
    @Modifying
    @Query("UPDATE WorkerInvitation i SET i.used = true WHERE i.email = :email AND i.company.id = :companyId AND i.used = false")
    void invalidatePreviousInvitations(@Param("email") String email, @Param("companyId") Long companyId);

    /**
     * Find all invitations by company (ordered by creation date descending)
     */
    @Query("SELECT i FROM WorkerInvitation i WHERE i.company.id = :companyId ORDER BY i.createdAt DESC")
    List<WorkerInvitation> findByCompanyIdOrderByCreatedAtDesc(@Param("companyId") Long companyId);

    /**
     * Cleanup expired and used invitations older than cutoff date
     */
    @Modifying
    @Query("DELETE FROM WorkerInvitation i WHERE i.used = true OR i.expiresAt < :cutoff")
    int deleteExpiredAndUsedInvitations(@Param("cutoff") LocalDateTime cutoff);
}
