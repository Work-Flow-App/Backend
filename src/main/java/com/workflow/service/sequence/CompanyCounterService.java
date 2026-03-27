package com.workflow.service.sequence;

import com.workflow.entity.CompanyCounters;
import com.workflow.repository.CompanyCountersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompanyCounterService {

    private final CompanyCountersRepository countersRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public long nextJobId(Long companyId) {
        CompanyCounters c = getOrCreate(companyId);
        long val = c.getNextJobId();
        c.setNextJobId(val + 1);
        countersRepository.save(c);
        return val;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public long nextWorkerId(Long companyId) {
        CompanyCounters c = getOrCreate(companyId);
        long val = c.getNextWorkerId();
        c.setNextWorkerId(val + 1);
        countersRepository.save(c);
        return val;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public long nextCustomerId(Long companyId) {
        CompanyCounters c = getOrCreate(companyId);
        long val = c.getNextCustomerId();
        c.setNextCustomerId(val + 1);
        countersRepository.save(c);
        return val;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public long nextClientId(Long companyId) {
        CompanyCounters c = getOrCreate(companyId);
        long val = c.getNextClientId();
        c.setNextClientId(val + 1);
        countersRepository.save(c);
        return val;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public long nextAssetId(Long companyId) {
        CompanyCounters c = getOrCreate(companyId);
        long val = c.getNextAssetId();
        c.setNextAssetId(val + 1);
        countersRepository.save(c);
        return val;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public long nextTemplateId(Long companyId) {
        CompanyCounters c = getOrCreate(companyId);
        long val = c.getNextTemplateId();
        c.setNextTemplateId(val + 1);
        countersRepository.save(c);
        return val;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public long nextWorkflowId(Long companyId) {
        CompanyCounters c = getOrCreate(companyId);
        long val = c.getNextWorkflowId();
        c.setNextWorkflowId(val + 1);
        countersRepository.save(c);
        return val;
    }

    private CompanyCounters getOrCreate(Long companyId) {
        return countersRepository.findByIdWithLock(companyId)
                .orElseGet(() -> CompanyCounters.builder()
                        .companyId(companyId)
                        .nextJobId(1L)
                        .nextWorkerId(1L)
                        .nextCustomerId(1L)
                        .nextClientId(1L)
                        .nextAssetId(1L)
                        .nextTemplateId(1L)
                        .nextWorkflowId(1L)
                        .build());
    }
}
