package com.workflow.service.company;

import java.util.List;

import com.workflow.dto.company.CompanyPostGroupRequest;
import com.workflow.dto.company.CompanyPostGroupResponse;

public interface ICompanyPostGroupService {
    CompanyPostGroupResponse createGroup(Long companyId, CompanyPostGroupRequest request);

    CompanyPostGroupResponse updateGroup(Long companyId, Long groupId, CompanyPostGroupRequest request);

    void deleteGroup(Long companyId, Long groupId);

    List<CompanyPostGroupResponse> getAllGroups(Long companyId);
}
