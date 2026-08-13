package com.workflow.service.company;

import com.workflow.common.constant.company.CompanyDocumentType;
import com.workflow.common.exception.business.CompanyDocumentNotFoundException;
import com.workflow.common.exception.business.CompanyNotFoundException;
import com.workflow.common.exception.business.CompanyPostNotFoundException;
import com.workflow.common.exception.business.EmptyFileException;
import com.workflow.common.exception.business.ForbiddenActionException;
import com.workflow.dto.company.*;
import com.workflow.entity.company.*;
import com.workflow.repository.company.*;
import com.workflow.service.storage.IStorageService;
import com.workflow.service.subscription.IStorageQuotaService;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CompanyProfileMediaService implements ICompanyProfileMediaService {

    private final CompanyRepository companyRepository;
    private final CompanyDocumentRepository documentRepository;
    private final CompanyPostRepository postRepository;
    private final IStorageService s3Service;
    private final IStorageQuotaService storageQuotaService;
    private final Tika tika;

    // ==========================================
    // LOGO MANAGEMENT
    // ==========================================

    @Override
    public String uploadLogo(Long companyId, MultipartFile file) throws IOException {
        if (file.isEmpty())
            throw new EmptyFileException("Logo file is empty");
        Company company = getCompany(companyId);

        String detectedType = tika.detect(file.getInputStream());
        if (!detectedType.startsWith("image/")) {
            throw new ForbiddenActionException("Only images are allowed for logos");
        }

        String extension = extractExtension(file.getOriginalFilename());
        String key = String.format("companies/%d/logo/%s", companyId, UUID.randomUUID() + extension);

        // Note: logo has no persisted size column (out of Phase 1 schema scope), so the old
        // logo's bytes can't be decremented here — this is a known one-way drift for logo only,
        // replacing a logo repeatedly will overcount usage. Flagged for a follow-up migration.
        if (company.getLogoUrl() != null) {
            s3Service.delete(company.getLogoUrl()); // Delete old
        }

        storageQuotaService.assertCapacity(companyId, file.getSize());
        s3Service.upload(key, file.getInputStream(), file.getSize(), detectedType);
        company.setLogoUrl(key);
        companyRepository.save(company);
        storageQuotaService.recordUpload(companyId, file.getSize());

        return s3Service.resolveFileUrl(key);
    }

    @Override
    public void deleteLogo(Long companyId) {
        Company company = getCompany(companyId);
        if (company.getLogoUrl() != null) {
            s3Service.delete(company.getLogoUrl());
            company.setLogoUrl(null);
            companyRepository.save(company);
        }
    }

    // ==========================================
    // DOCUMENTS CRUD
    // ==========================================

    @Override
    public CompanyDocumentResponse uploadDocument(
            Long companyId, MultipartFile file, String title, String description,
            CompanyDocumentType type, LocalDate startDate, LocalDate endDate, boolean isPublic) throws IOException {

        Company company = getCompany(companyId);
        String detectedType = tika.detect(file.getInputStream());
        String originalName = StringUtils.cleanPath(file.getOriginalFilename());
        String key = String.format("companies/%d/documents/%s", companyId,
                UUID.randomUUID() + extractExtension(originalName));

        storageQuotaService.assertCapacity(companyId, file.getSize());
        s3Service.upload(key, file.getInputStream(), file.getSize(), detectedType);

        CompanyDocument document = documentRepository.save(CompanyDocument.builder()
                .company(company)
                .title(title)
                .description(description)
                .type(type)
                .fileUrl(key)
                .fileName(originalName)
                .fileType(detectedType)
                .fileSizeBytes(file.getSize())
                .validityStartDate(startDate)
                .validityEndDate(endDate)
                .isPublic(isPublic)
                .build());
        storageQuotaService.recordUpload(companyId, file.getSize());

        return map(document);
    }

    @Override
    public CompanyDocumentResponse updateDocument(
            Long companyId, Long documentId, MultipartFile newFile,
            String title, String description, CompanyDocumentType type,
            LocalDate startDate, LocalDate endDate, Boolean isPublic) throws IOException {

        CompanyDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new CompanyDocumentNotFoundException("Document not found"));

        verifyCompanyOwnership(companyId, doc.getCompany().getId());

        if (title != null)
            doc.setTitle(title);
        if (description != null)
            doc.setDescription(description);
        if (type != null)
            doc.setType(type);
        if (startDate != null)
            doc.setValidityStartDate(startDate);
        if (endDate != null)
            doc.setValidityEndDate(endDate);
        if (isPublic != null)
            doc.setPublic(isPublic);

        if (newFile != null && !newFile.isEmpty()) {
            // Delete old file
            Long previousFileSizeBytes = doc.getFileSizeBytes();
            s3Service.delete(doc.getFileUrl());
            // Legacy rows uploaded before fileSizeBytes existed have nothing to decrement
            if (previousFileSizeBytes != null) {
                storageQuotaService.recordDelete(companyId, previousFileSizeBytes);
            }

            // Upload new file
            String detectedType = tika.detect(newFile.getInputStream());
            String originalName = StringUtils.cleanPath(newFile.getOriginalFilename());
            String key = String.format("companies/%d/documents/%s", companyId,
                    UUID.randomUUID() + extractExtension(originalName));
            storageQuotaService.assertCapacity(companyId, newFile.getSize());
            s3Service.upload(key, newFile.getInputStream(), newFile.getSize(), detectedType);
            storageQuotaService.recordUpload(companyId, newFile.getSize());

            doc.setFileUrl(key);
            doc.setFileName(originalName);
            doc.setFileType(detectedType);
            doc.setFileSizeBytes(newFile.getSize());
        }

        return map(documentRepository.save(doc));
    }

    @Override
    public void deleteDocument(Long companyId, Long documentId) {
        CompanyDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new CompanyDocumentNotFoundException("Document not found"));

        verifyCompanyOwnership(companyId, doc.getCompany().getId());

        s3Service.delete(doc.getFileUrl());
        // Legacy rows uploaded before fileSizeBytes existed have nothing to decrement
        if (doc.getFileSizeBytes() != null) {
            storageQuotaService.recordDelete(companyId, doc.getFileSizeBytes());
        }
        documentRepository.delete(doc);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyDocumentResponse> getDocuments(Long companyId, boolean publicOnly) {
        List<CompanyDocument> docs = publicOnly
                ? documentRepository.findByCompanyIdAndIsPublicTrueOrderByCreatedAtDesc(companyId)
                : documentRepository.findByCompanyIdOrderByCreatedAtDesc(companyId);
        return docs.stream().map(this::map).toList();
    }

    // ==========================================
    // POSTS CRUD
    // ==========================================

    @Override
    public CompanyPostResponse createPost(Long companyId, CompanyPostCreateRequest request, List<MultipartFile> files)
            throws IOException {
        Company company = getCompany(companyId);

        CompanyPost post = CompanyPost.builder()
                .company(company)
                .author(company.getUser())
                .content(request.content())
                .isPublic(request.isPublic())
                .build();

        post = postRepository.save(post);

        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (file.isEmpty())
                    continue;
                uploadPostAttachment(companyId, post, file);
            }
            post = postRepository.save(post);
        }
        return map(post);
    }

    @Override
    public CompanyPostResponse updatePost(Long companyId, Long postId, CompanyPostUpdateRequest request,
            List<MultipartFile> newFiles) throws IOException {
        CompanyPost post = postRepository.findById(postId)
                .orElseThrow(() -> new CompanyPostNotFoundException("Post not found"));

        verifyCompanyOwnership(companyId, post.getCompany().getId());

        if (request != null) {
            if (request.content() != null)
                post.setContent(request.content());
            if (request.isPublic() != null)
                post.setPublic(request.isPublic());

            // Delete specified attachments
            if (request.attachmentIdsToDelete() != null && !request.attachmentIdsToDelete().isEmpty()) {
                List<CompanyPostAttachment> toRemove = post.getAttachments().stream()
                        .filter(a -> request.attachmentIdsToDelete().contains(a.getId()))
                        .toList();

                for (CompanyPostAttachment attachment : toRemove) {
                    s3Service.delete(attachment.getFileUrl()); // delete from S3
                    // Legacy rows uploaded before fileSizeBytes existed have nothing to decrement
                    if (attachment.getFileSizeBytes() != null) {
                        storageQuotaService.recordDelete(companyId, attachment.getFileSizeBytes());
                    }
                    post.getAttachments().remove(attachment); // orphanRemoval=true will delete from DB
                }
            }
        }

        // Add new files
        if (newFiles != null && !newFiles.isEmpty()) {
            for (MultipartFile file : newFiles) {
                if (!file.isEmpty()) {
                    uploadPostAttachment(companyId, post, file);
                }
            }
        }

        return map(postRepository.save(post));
    }

    @Override
    public void deletePost(Long companyId, Long postId) {
        CompanyPost post = postRepository.findById(postId)
                .orElseThrow(() -> new CompanyPostNotFoundException("Post not found"));

        verifyCompanyOwnership(companyId, post.getCompany().getId());

        // Delete all associated files from S3
        post.getAttachments().forEach(attachment -> {
            s3Service.delete(attachment.getFileUrl());
            // Legacy rows uploaded before fileSizeBytes existed have nothing to decrement
            if (attachment.getFileSizeBytes() != null) {
                storageQuotaService.recordDelete(companyId, attachment.getFileSizeBytes());
            }
        });

        postRepository.delete(post);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyPostResponse> getPosts(Long companyId, boolean publicOnly) {
        List<CompanyPost> posts = publicOnly
                ? postRepository.findByCompanyIdAndIsPublicTrueOrderByCreatedAtDesc(companyId)
                : postRepository.findByCompanyIdOrderByCreatedAtDesc(companyId);
        return posts.stream().map(this::map).toList();
    }

    // ==========================================
    // PUBLIC PROFILE VIEW
    // ==========================================

    @Override
    @Transactional(readOnly = true)
    public PublicCompanyProfileResponse getPublicProfile(Long companyId) {
        Company company = companyRepository.findByIdAndNotArchived(companyId)
                .orElseThrow(() -> new CompanyNotFoundException("Company not found"));

        return new PublicCompanyProfileResponse(
                company.getId(),
                company.getName(),
                company.getLogoUrl() != null ? s3Service.resolveFileUrl(company.getLogoUrl()) : null,
                company.getDescription(),
                company.getWebsite(),
                company.getTagline(),
                CompanyAddressResponse.fromEntity(company.getAddress()));
    }

    // ==========================================
    // PRIVATE HELPERS
    // ==========================================

    private void uploadPostAttachment(Long companyId, CompanyPost post, MultipartFile file) throws IOException {
        String detectedType = tika.detect(file.getInputStream());
        String originalName = StringUtils.cleanPath(file.getOriginalFilename());
        String key = String.format("companies/%d/posts/%d/%s", companyId, post.getId(),
                UUID.randomUUID() + extractExtension(originalName));

        storageQuotaService.assertCapacity(companyId, file.getSize());
        s3Service.upload(key, file.getInputStream(), file.getSize(), detectedType);
        storageQuotaService.recordUpload(companyId, file.getSize());

        post.getAttachments().add(CompanyPostAttachment.builder()
                .post(post)
                .fileUrl(key)
                .fileName(originalName)
                .fileType(detectedType)
                .fileSizeBytes(file.getSize())
                .build());
    }

    private Company getCompany(Long companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new CompanyNotFoundException("Company not found"));
    }

    private void verifyCompanyOwnership(Long requestCompanyId, Long resourceCompanyId) {
        if (!requestCompanyId.equals(resourceCompanyId)) {
            throw new ForbiddenActionException("You do not have permission to modify this resource");
        }
    }

    private String extractExtension(String filename) {
        return (filename != null && filename.contains("."))
                ? filename.substring(filename.lastIndexOf("."))
                : "";
    }

    private CompanyDocumentResponse map(CompanyDocument d) {
        return new CompanyDocumentResponse(
                d.getId(), d.getTitle(), d.getDescription(), d.getType(),
                s3Service.resolveFileUrl(d.getFileUrl()), d.getFileName(),
                d.getValidityStartDate(), d.getValidityEndDate(), d.isPublic(), d.getCreatedAt());
    }

    private CompanyPostResponse map(CompanyPost p) {
        return new CompanyPostResponse(
                p.getId(), p.getContent(), p.isPublic(), p.getAuthor().getUsername(),
                p.getAttachments().stream().map(a -> new CompanyPostAttachmentResponse(
                        a.getId(), s3Service.resolveFileUrl(a.getFileUrl()), a.getFileName(), a.getFileType()))
                        .toList(),
                p.getCreatedAt());
    }
}