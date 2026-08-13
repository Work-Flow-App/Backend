package com.workflow.repository.company;

import com.workflow.entity.company.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    @Query("SELECT COUNT(w.id) FROM Worker w WHERE w.company.id = :companyId")
    long countWorkers(@Param("companyId") Long companyId);

    @Query("SELECT COUNT(w.id) FROM Worker w WHERE w.company.id = :companyId AND w.archived = false")
    long countActiveWorkers(@Param("companyId") Long companyId);

    @Query("SELECT COUNT(c.id) FROM Client c WHERE c.company.id = :companyId")
    long countClients(@Param("companyId") Long companyId);

    @Query("SELECT c.storageUsedBytes FROM Company c WHERE c.id = :companyId")
    Optional<Long> findStorageUsedBytes(@Param("companyId") Long companyId);

    // No entity load — avoids lock contention with concurrent uploads/deletes on the same company row.
    // flushAutomatically makes the "runs before any pending Company changes in this tx are lost" guarantee
    // explicit rather than relying on Hibernate's incidental same-table auto-flush behavior.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Company c SET c.storageUsedBytes = c.storageUsedBytes + :bytes WHERE c.id = :companyId")
    void incrementStorageUsedBytes(@Param("companyId") Long companyId, @Param("bytes") long bytes);

    // Floors at 0 defensively — a double-delete or size-tracking bug must not push usage negative
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Company c SET c.storageUsedBytes = GREATEST(0, c.storageUsedBytes - :bytes) WHERE c.id = :companyId")
    void decrementStorageUsedBytes(@Param("companyId") Long companyId, @Param("bytes") long bytes);
}
