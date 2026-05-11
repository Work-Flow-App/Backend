package com.workflow.repository.asset;

import com.workflow.entity.asset.AssetJobAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AssetJobAssignmentRepository extends JpaRepository<AssetJobAssignment, Long> {
    List<AssetJobAssignment> findByAssetIdOrderByAssignedAtDesc(Long assetId);
    List<AssetJobAssignment> findTop10ByAssetIdOrderByAssignedAtDesc(Long assetId);
    Optional<AssetJobAssignment> findByAssetIdAndReturnedAtIsNull(Long assetId);
    Optional<AssetJobAssignment> findByIdAndAssetId(Long id, Long assetId);
    List<AssetJobAssignment> findByJobIdAndReturnedAtIsNull(Long jobId);

    List<AssetJobAssignment> findByJobIdInAndReturnedAtIsNull(List<Long> jobIds);
    List<AssetJobAssignment> findByAssetIdInAndReturnedAtIsNull(List<Long> assetIds);
    List<AssetJobAssignment> findByJobId(Long jobId);

    @Modifying
    @Query("DELETE FROM AssetJobAssignment a WHERE a.job.id = :jobId")
    void deleteByJobId(@Param("jobId") Long jobId);
}
