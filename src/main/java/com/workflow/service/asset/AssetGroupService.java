package com.workflow.service.asset;

import com.workflow.common.exception.business.DuplicateNameException;
import com.workflow.common.exception.business.InvalidRequestException;
import com.workflow.dto.asset.AssetGroupCreateRequest;
import com.workflow.dto.asset.AssetGroupResponse;
import com.workflow.entity.asset.AssetGroup;
import com.workflow.repository.asset.AssetGroupRepository;
import com.workflow.repository.asset.AssetRepository;
import com.workflow.repository.company.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AssetGroupService implements IAssetGroupService {

    private final AssetGroupRepository groupRepository;
    private final CompanyRepository companyRepository;
    private final AssetRepository assetRepository;

    @Override
    public AssetGroupResponse createGroup(AssetGroupCreateRequest request, Long companyId) {
        if (groupRepository.existsByCompanyIdAndName(companyId, request.getName())) {
            throw new DuplicateNameException("Group name already exists");
        }

        AssetGroup group = AssetGroup.builder()
                .company(companyRepository.getReferenceById(companyId))
                .name(request.getName())
                .description(request.getDescription())
                .build();

        return mapToResponse(groupRepository.save(group));
    }

    @Override
    public AssetGroupResponse updateGroup(Long groupId, AssetGroupCreateRequest request, Long companyId) {
        AssetGroup group = groupRepository.findById(groupId)
                .filter(g -> g.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new InvalidRequestException("Group not found"));

        if (groupRepository.existsByCompanyIdAndNameAndIdNot(companyId, request.getName(), groupId)) {
            throw new DuplicateNameException("Group name already exists");
        }

        group.setName(request.getName());
        group.setDescription(request.getDescription());
        return mapToResponse(groupRepository.save(group));
    }

    @Override
    public void deleteGroup(Long groupId, Long companyId) {
        AssetGroup group = groupRepository.findById(groupId)
                .filter(g -> g.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new InvalidRequestException("Group not found"));
        // Check if assets are currently using this group
        if (assetRepository.existsByCompanyIdAndAssetGroupId(companyId, groupId)) {
            throw new InvalidRequestException(
                    "Cannot delete this group because it contains assets. Please reassign or remove the assets first.");
        }

        groupRepository.delete(group);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssetGroupResponse> listGroups(Long companyId, int page, int size) {
        return groupRepository.findByCompanyId(companyId, PageRequest.of(page, size, Sort.by("name")))
                .map(this::mapToResponse);
    }

    private AssetGroupResponse mapToResponse(AssetGroup g) {
        return AssetGroupResponse.builder()
                .id(g.getId())
                .companyId(g.getCompany().getId())
                .name(g.getName())
                .description(g.getDescription())
                .createdAt(g.getCreatedAt())
                .updatedAt(g.getUpdatedAt())
                .build();
    }
}