package com.workflow.service.admin;

import com.workflow.dto.admin.AdminCompanyResponse;
import com.workflow.dto.admin.AdminJobResponse;
import com.workflow.dto.admin.AdminWorkerResponse;
import com.workflow.repository.company.CompanyRepository;
import com.workflow.repository.job.JobRepository;
import com.workflow.repository.worker.WorkerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class AdminService implements IAdminService {

    private final CompanyRepository companyRepository;
    private final WorkerRepository  workerRepository;
    private final JobRepository     jobRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<AdminCompanyResponse> getAllCompanies(Pageable pageable) {
        return companyRepository.findAll(pageable)
                .map(c -> new AdminCompanyResponse(
                        c.getId(),
                        c.getName(),
                        c.getEmail(),
                        c.getCreatedAt()
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminWorkerResponse> getAllWorkers(Pageable pageable) {
        return workerRepository.findAll(pageable)
                .map(w -> new AdminWorkerResponse(
                        w.getId(),
                        w.getName(),
                        w.getEmail(),
                        w.getCompany().getId(),
                        w.getCreatedAt()
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminJobResponse> getAllJobs(Pageable pageable) {
        return jobRepository.findAllWithCompanyAndTemplate(pageable)
                .map(j -> new AdminJobResponse(
                        j.getId(),
                        j.getTemplate().getName(),
                        j.getStatus(),
                        j.getCompany().getId(),
                        j.getCreatedAt()
                ));
    }
}
