package com.workflow.repository.company;

import com.workflow.entity.company.CompanyMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CompanyMemberRepository extends JpaRepository<CompanyMember, Long> {

    @Query("SELECT cm FROM CompanyMember cm JOIN FETCH cm.company WHERE cm.user.id = :userId AND cm.active = true")
    Optional<CompanyMember> findByUserId(@Param("userId") Long userId);

    Optional<CompanyMember> findByCompanyIdAndUserId(Long companyId, Long userId);

    @Query("SELECT cm FROM CompanyMember cm JOIN FETCH cm.user WHERE cm.company.id = :companyId AND cm.active = true")
    List<CompanyMember> findActiveByCompanyId(@Param("companyId") Long companyId);

    boolean existsByCompanyIdAndUserId(Long companyId, Long userId);
}
