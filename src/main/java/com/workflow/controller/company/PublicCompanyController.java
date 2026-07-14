package com.workflow.controller.company;

import com.workflow.dto.company.CompanyDocumentResponse;
import com.workflow.dto.company.CompanyPostResponse;
import com.workflow.dto.company.PublicCompanyProfileResponse;
import com.workflow.service.company.ICompanyProfileMediaService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Public Company Views")
@RestController
@RequestMapping("/api/v1/public/companies")
@RequiredArgsConstructor
public class PublicCompanyController {

    private final ICompanyProfileMediaService mediaService;

    @GetMapping("/{companyId}/profile")
    public ResponseEntity<PublicCompanyProfileResponse> getPublicProfile(@PathVariable Long companyId) {
        return ResponseEntity.ok(mediaService.getPublicProfile(companyId));
    }

    @GetMapping("/{companyId}/documents")
    public ResponseEntity<List<CompanyDocumentResponse>> getPublicDocuments(@PathVariable Long companyId) {
        return ResponseEntity.ok(mediaService.getDocuments(companyId, true)); // true = public only
    }

    @GetMapping("/{companyId}/posts")
    public ResponseEntity<List<CompanyPostResponse>> getPublicPosts(@PathVariable Long companyId) {
        return ResponseEntity.ok(mediaService.getPosts(companyId, true)); // true = public only
    }
}