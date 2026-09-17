package com.workflow.service.company;

import com.workflow.common.exception.business.CompanyPostGroupNotFoundException;
import com.workflow.common.exception.business.ForbiddenActionException;
import com.workflow.common.exception.business.GroupInUseException;
import com.workflow.dto.company.CompanyPostGroupRequest;
import com.workflow.dto.company.CompanyPostGroupResponse;
import com.workflow.entity.company.Company;
import com.workflow.entity.company.CompanyPostGroup;
import com.workflow.repository.company.CompanyPostGroupRepository;
import com.workflow.repository.company.CompanyPostRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
class CompanyPostGroupService implements ICompanyPostGroupService {

    private final CompanyPostGroupRepository groupRepository;
    private final CompanyPostRepository postRepository;
    private final ICompanyService companyService;

    @Override
    public CompanyPostGroupResponse createGroup(Long companyId, CompanyPostGroupRequest request) {
        Company company = companyService.findCompanyByUserId(companyId); // Adjust based on your company lookup logic
        
        if (groupRepository.existsByCompanyIdAndNameIgnoreCase(companyId, request.name())) {
            throw new ForbiddenActionException("Group name already exists for this company");
        }

        CompanyPostGroup group = CompanyPostGroup.builder()
                .company(company)
                .name(request.name())
                .description(request.description())
                .build();

        return map(groupRepository.save(group));
    }

    @Override
    public CompanyPostGroupResponse updateGroup(Long companyId, Long groupId, CompanyPostGroupRequest request) {
        CompanyPostGroup group = groupRepository.findByIdAndCompanyId(groupId, companyId)
                .orElseThrow(() -> new CompanyPostGroupNotFoundException("Group not found"));

        if (!group.getName().equalsIgnoreCase(request.name()) && 
            groupRepository.existsByCompanyIdAndNameIgnoreCase(companyId, request.name())) {
            throw new ForbiddenActionException("Group name already exists for this company");
        }

        group.setName(request.name());
        group.setDescription(request.description());

        return map(groupRepository.save(group));
    }

    @Override
    public void deleteGroup(Long companyId, Long groupId) {
        CompanyPostGroup group = groupRepository.findByIdAndCompanyId(groupId, companyId)
                .orElseThrow(() -> new CompanyPostGroupNotFoundException("Group not found"));

        // Crucial Check: Ensure we don't delete a group tied to posts
        if (postRepository.existsByGroupId(groupId)) {
            throw new GroupInUseException("Cannot delete group because it contains active posts. Reassign or delete the posts first.");
        }

        groupRepository.delete(group);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyPostGroupResponse> getAllGroups(Long companyId) {
        return groupRepository.findByCompanyIdOrderByNameAsc(companyId)
                .stream().map(this::map).toList();
    }

    private CompanyPostGroupResponse map(CompanyPostGroup group) {
        return new CompanyPostGroupResponse(group.getId(), group.getName(), group.getDescription(), group.getCreatedAt());
    }
}
