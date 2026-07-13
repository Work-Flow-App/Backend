package com.workflow.service.asset;

import com.workflow.dto.asset.*;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

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

    AssetResponse addAttachments(Long assetId, List<MultipartFile> files, Long companyId);

    AssetResponse removeAttachment(Long assetId, String fileUrl, Long companyId);
}
