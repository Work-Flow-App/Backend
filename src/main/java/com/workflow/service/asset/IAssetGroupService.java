package com.workflow.service.asset;

import com.workflow.dto.asset.AssetGroupCreateRequest;
import com.workflow.dto.asset.AssetGroupResponse;
import org.springframework.data.domain.Page;

public interface IAssetGroupService {
    AssetGroupResponse createGroup(AssetGroupCreateRequest request, Long companyId);

    AssetGroupResponse updateGroup(Long groupId, AssetGroupCreateRequest request, Long companyId);

    void deleteGroup(Long groupId, Long companyId);

    Page<AssetGroupResponse> listGroups(Long companyId, int page, int size);
}