package com.workflow.service.subscription;

public interface IStorageQuotaService {

    void assertCapacity(Long companyId, long incomingBytes);

    void recordUpload(Long companyId, long bytes);

    void recordDelete(Long companyId, long bytes);
}
