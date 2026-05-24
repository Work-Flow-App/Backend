package com.workflow.service.estimatedocument;

import com.workflow.dto.estimatedocument.EstimateDocumentCreateRequest;
import com.workflow.dto.estimatedocument.EstimateDocumentResponse;

import java.util.List;

public interface IEstimateDocumentService {
    EstimateDocumentResponse generateEstimateDocument(Long estimateId, EstimateDocumentCreateRequest request, Long companyId);
    List<EstimateDocumentResponse> getEstimateDocuments(Long estimateId, Long companyId);
    EstimateDocumentResponse getEstimateDocument(Long documentId, Long companyId);
}
