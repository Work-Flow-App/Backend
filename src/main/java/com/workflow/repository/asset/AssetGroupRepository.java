package com.workflow.repository.asset;

import com.workflow.entity.asset.AssetGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetGroupRepository extends JpaRepository<AssetGroup, Long> {
    boolean existsByCompanyIdAndName(Long companyId, String name);

    boolean existsByCompanyIdAndNameAndIdNot(Long companyId, String name, Long id);

    Page<AssetGroup> findByCompanyId(Long companyId, Pageable pageable);
}