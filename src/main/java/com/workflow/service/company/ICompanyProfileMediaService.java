package com.workflow.service.company;

import com.workflow.common.constant.company.CompanyDocumentType;
import com.workflow.dto.company.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

public interface ICompanyProfileMediaService {

    // --- LOGO ---
    String uploadLogo(Long companyId, MultipartFile file) throws IOException;

    void deleteLogo(Long companyId);

    // --- DOCUMENTS ---
    CompanyDocumentResponse uploadDocument(Long companyId, MultipartFile file, String title,
            String description, CompanyDocumentType type,
            LocalDate startDate, LocalDate endDate, boolean isPublic) throws IOException;

    CompanyDocumentResponse updateDocument(Long companyId, Long documentId, MultipartFile newFile,
            String title, String description, CompanyDocumentType type,
            LocalDate startDate, LocalDate endDate, Boolean isPublic) throws IOException;

    void deleteDocument(Long companyId, Long documentId);

    List<CompanyDocumentResponse> getDocuments(Long companyId, boolean publicOnly);

    // --- POSTS ---
    CompanyPostResponse createPost(Long companyId, CompanyPostCreateRequest request,
            List<MultipartFile> files) throws IOException;

    CompanyPostResponse updatePost(Long companyId, Long postId, CompanyPostUpdateRequest request,
            List<MultipartFile> newFiles) throws IOException;

    void deletePost(Long companyId, Long postId);

    List<CompanyPostResponse> getPosts(Long companyId, boolean publicOnly);

    // --- PUBLIC PROFILE ---
    PublicCompanyProfileResponse getPublicProfile(Long companyId);
}