package com.workflow.repository;

import com.workflow.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    Optional<Company> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    @Query("SELECT c FROM Company c WHERE c.user.id = :userId AND c.archived = false")
    Optional<Company> findByUserIdAndNotArchived(@Param("userId") Long userId);

    @Query("SELECT c FROM Company c WHERE c.id = :companyId AND c.archived = false")
    Optional<Company> findByIdAndNotArchived(@Param("companyId") Long companyId);
}
