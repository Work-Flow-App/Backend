package com.workflow.service.admin;

import com.workflow.dto.admin.AdminCompanyResponse;
import com.workflow.dto.admin.AdminJobResponse;
import com.workflow.dto.admin.AdminWorkerResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IAdminService {

    Page<AdminCompanyResponse> getAllCompanies(Pageable pageable);

    Page<AdminWorkerResponse> getAllWorkers(Pageable pageable);

    Page<AdminJobResponse> getAllJobs(Pageable pageable);
}
