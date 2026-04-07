package com.workflow.repository;

import com.workflow.entity.Asset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AssetRepository extends JpaRepository<Asset, Long> {
    boolean existsByCompanyIdAndName(Long companyId, String name);
    boolean existsByCompanyIdAndAssetTag(Long companyId, String assetTag);
    Page<Asset> findByCompanyIdAndArchivedFalse(Long companyId, Pageable pageable);
    Page<Asset> findByCompanyId(Long companyId, Pageable pageable);
    Page<Asset> findByCompanyIdAndArchivedFalseAndAvailable(Long companyId, boolean available, Pageable pageable);

    @Query("SELECT COUNT(a) FROM Asset a WHERE a.company.id = :companyId AND a.archived = false")
    long countActiveByCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT COUNT(a) FROM Asset a WHERE a.company.id = :companyId AND a.archived = false AND a.available = true")
    long countAvailableByCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT a FROM Asset a WHERE a.company.id = :companyId AND a.archived = false")
    List<Asset> findActiveByCompanyId(@Param("companyId") Long companyId);
}
