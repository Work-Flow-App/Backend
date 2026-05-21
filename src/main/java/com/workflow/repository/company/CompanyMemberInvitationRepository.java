package com.workflow.repository.company;

import com.workflow.entity.company.CompanyMemberInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CompanyMemberInvitationRepository extends JpaRepository<CompanyMemberInvitation, Long> {

    Optional<CompanyMemberInvitation> findByInvitationToken(String token);

    List<CompanyMemberInvitation> findByCompanyIdOrderByCreatedAtDesc(Long companyId);

    @Modifying
    @Query("UPDATE CompanyMemberInvitation i SET i.used = true WHERE i.email = :email AND i.company.id = :companyId AND i.used = false")
    void invalidatePreviousInvitations(@Param("email") String email, @Param("companyId") Long companyId);
}
