package com.workflow.controller.company;

import com.workflow.common.constant.company.CompanyDocumentType;
import com.workflow.common.security.RequireCompanyRole;
import com.workflow.common.util.AuthUtils;
import com.workflow.dto.company.CompanyDashboardResponse;
import com.workflow.dto.company.CompanyDocumentResponse;
import com.workflow.dto.company.CompanyPostCreateRequest;
import com.workflow.dto.company.CompanyPostResponse;
import com.workflow.dto.company.CompanyPostUpdateRequest;
import com.workflow.dto.company.CompanyProfileResponse;
import com.workflow.dto.company.CompanyProfileUpdateRequest;
import com.workflow.dto.company.UsageSummaryResponse;
import com.workflow.entity.auth.User;
import com.workflow.service.company.ICompanyProfileMediaService;
import com.workflow.service.company.ICompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.tags.Tag;

import static com.workflow.common.constant.CompanyRole.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Tag(name = "Company")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/companies")
public class CompanyController {

    private final ICompanyService companyService;
    private final ICompanyProfileMediaService mediaService;

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER})
    @PostMapping("/profile")
    public ResponseEntity<CompanyProfileResponse> updateProfile(
            @Valid @RequestBody CompanyProfileUpdateRequest request,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(companyService.updateProfile(request, user.getId()));
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR, VIEWER})
    @GetMapping("/profile")
    public ResponseEntity<CompanyProfileResponse> getProfile(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(companyService.getProfile(user.getId()));
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR, VIEWER})
    @GetMapping("/dashboard")
    public ResponseEntity<CompanyDashboardResponse> getDashboard(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(companyService.getDashboard(user.getId()));
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR, VIEWER})
    @GetMapping("/usage")
    public ResponseEntity<UsageSummaryResponse> getUsage(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(companyService.getUsageSummary(user.getId()));
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER})
    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadLogo(@RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(mediaService.uploadLogo(AuthUtils.getCompanyId(), file));
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER})
    @DeleteMapping("/logo")
    public ResponseEntity<Void> deleteLogo() {
        mediaService.deleteLogo(AuthUtils.getCompanyId());
        return ResponseEntity.noContent().build();
    }

    // --- DOCUMENTS ---

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR})
    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CompanyDocumentResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("type") CompanyDocumentType type,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam("isPublic") boolean isPublic) throws IOException {
        
        return ResponseEntity.ok(mediaService.uploadDocument(
                AuthUtils.getCompanyId(), file, title, description, type, startDate, endDate, isPublic));
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR})
    @PutMapping(value = "/documents/{documentId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CompanyDocumentResponse> updateDocument(
            @PathVariable Long documentId,
            @RequestParam(value = "file", required = false) MultipartFile newFile,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "type", required = false) CompanyDocumentType type,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "isPublic", required = false) Boolean isPublic) throws IOException {
        
        return ResponseEntity.ok(mediaService.updateDocument(
                AuthUtils.getCompanyId(), documentId, newFile, title, description, type, startDate, endDate, isPublic));
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER})
    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long documentId) {
        mediaService.deleteDocument(AuthUtils.getCompanyId(), documentId);
        return ResponseEntity.noContent().build();
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR, VIEWER})
    @GetMapping("/documents")
    public ResponseEntity<List<CompanyDocumentResponse>> getCompanyDocuments() {
        return ResponseEntity.ok(mediaService.getDocuments(AuthUtils.getCompanyId(), false));
    }

    // --- POSTS ---

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR})
    @PostMapping(value = "/posts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CompanyPostResponse> createPost(
            @RequestPart("data") @Valid CompanyPostCreateRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) throws IOException {
        
        return ResponseEntity.ok(mediaService.createPost(AuthUtils.getCompanyId(), request, files));
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR})
    @PutMapping(value = "/posts/{postId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CompanyPostResponse> updatePost(
            @PathVariable Long postId,
            @RequestPart(value = "data", required = false) CompanyPostUpdateRequest request,
            @RequestPart(value = "newFiles", required = false) List<MultipartFile> newFiles) throws IOException {
        
        return ResponseEntity.ok(mediaService.updatePost(AuthUtils.getCompanyId(), postId, request, newFiles));
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER})
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable Long postId) {
        mediaService.deletePost(AuthUtils.getCompanyId(), postId);
        return ResponseEntity.noContent().build();
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR, VIEWER})
    @GetMapping("/posts")
    public ResponseEntity<List<CompanyPostResponse>> getCompanyPosts() {
        return ResponseEntity.ok(mediaService.getPosts(AuthUtils.getCompanyId(), false));
    }
}
