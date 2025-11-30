package com.workflow.service.asset;

import com.workflow.dto.asset.*;
import org.springframework.data.domain.Page;
import java.time.LocalDate;

public interface IAssetService {
    AssetResponse createAsset(AssetCreateRequest request, Long companyId);

    AssetResponse updateAsset(Long assetId, AssetUpdateRequest request, Long companyId);

    AssetResponse getAsset(Long assetId, Long companyId);

    Page<AssetResponse> listAssets(Long companyId, int page, int size, Boolean archived, Boolean available,
            String sortBy, String direction);

    void archiveAsset(Long assetId, Long companyId);

    AssetValueResponse calculateAssetValue(Long assetId, Long companyId, LocalDate asOfDate);

    // Dashboard helpers
    AssetStatistics getStatistics(Long companyId);
}
