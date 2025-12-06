package com.workflow.repository;

import com.workflow.entity.Asset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetRepository extends JpaRepository<Asset, Long> {
    boolean existsByCompanyIdAndName(Long companyId, String name);
    boolean existsByCompanyIdAndAssetTag(Long companyId, String assetTag);
    Page<Asset> findByCompanyIdAndArchivedFalse(Long companyId, Pageable pageable);
    Page<Asset> findByCompanyId(Long companyId, Pageable pageable);
    Page<Asset> findByCompanyIdAndArchivedFalseAndAvailable(Long companyId, boolean available, Pageable pageable);
}
