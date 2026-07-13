package com.workflow.controller.asset;

import com.workflow.common.exception.business.InvalidRequestException;
import com.workflow.common.security.RequireCompanyRole;
import com.workflow.common.util.AuthUtils;
import com.workflow.dto.asset.*;
import com.workflow.service.asset.IAssetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import static com.workflow.common.constant.CompanyRole.*;

@Tag(name = "Assets")
@RestController
@RequestMapping("/api/v1/assets")
@RequiredArgsConstructor
public class AssetController {

    private final IAssetService service;

    private Long getCompanyId() {
        return AuthUtils.getCompanyId();
    }

    @RequireCompanyRole({ COMPANY_ADMIN, MANAGER, EDITOR })
    @PostMapping
    public ResponseEntity<AssetResponse> create(@Valid @RequestBody AssetCreateRequest request, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createAsset(request, getCompanyId()));
    }

    @RequireCompanyRole({ COMPANY_ADMIN, MANAGER, EDITOR })
    @PutMapping("/{id}")
    public ResponseEntity<AssetResponse> update(@PathVariable Long id, @Valid @RequestBody AssetUpdateRequest request,
            Authentication auth) {
        return ResponseEntity.ok(service.updateAsset(id, request, getCompanyId()));
    }

    @RequireCompanyRole({ COMPANY_ADMIN, MANAGER, EDITOR, VIEWER })
    @GetMapping("/{id}")
    public ResponseEntity<AssetResponse> get(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(service.getAsset(id, getCompanyId()));
    }

    @RequireCompanyRole({ COMPANY_ADMIN, MANAGER, EDITOR, VIEWER })
    @GetMapping
    public ResponseEntity<Page<AssetResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean archived,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "asc") String dir,
            Authentication auth) {
        return ResponseEntity.ok(service.listAssets(getCompanyId(), page, size, archived, available, sort, dir));
    }

    @RequireCompanyRole({ COMPANY_ADMIN, MANAGER })
    @PatchMapping("/{id}/archive")
    public ResponseEntity<Void> archive(@PathVariable Long id, Authentication auth) {
        service.archiveAsset(id, getCompanyId());
        return ResponseEntity.noContent().build();
    }

    @RequireCompanyRole({ COMPANY_ADMIN, MANAGER, EDITOR, VIEWER })
    @GetMapping("/{id}/value")
    public ResponseEntity<AssetValueResponse> value(
            @PathVariable Long id,
            @RequestParam(required = false) String asOf,
            Authentication auth) {
        LocalDate date = null;
        if (asOf != null) {
            try {
                date = LocalDate.parse(asOf);
            } catch (DateTimeParseException e) {
                throw new InvalidRequestException("Invalid date format. Expected yyyy-MM-dd, got: " + asOf);
            }
        }
        return ResponseEntity.ok(service.calculateAssetValue(id, getCompanyId(), date));
    }

    @RequireCompanyRole({ COMPANY_ADMIN, MANAGER, EDITOR, VIEWER })
    @GetMapping("/statistics")
    public ResponseEntity<AssetStatistics> stats(Authentication auth) {
        return ResponseEntity.ok(service.getStatistics(getCompanyId()));
    }

    @RequireCompanyRole({ COMPANY_ADMIN, MANAGER, EDITOR })
    @PostMapping(value = "/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AssetResponse> addAttachments(
            @PathVariable Long id,
            @RequestParam("files") List<MultipartFile> files,
            Authentication auth) {

        return ResponseEntity.ok(service.addAttachments(id, files, getCompanyId()));
    }

    @RequireCompanyRole({ COMPANY_ADMIN, MANAGER, EDITOR })
    @DeleteMapping("/{id}/attachments")
    public ResponseEntity<AssetResponse> removeAttachment(
            @PathVariable Long id,
            @RequestParam("fileUrl") String fileUrl,
            Authentication auth) {

        // Pass the raw S3 key (fileUrl) that the frontend received in the AssetResponse
        // If the user clicks the "X" on a specific image in the UI, the frontend hits 
        // DELETE /api/v1/assets/5/attachments?fileUrl=companies/1/assets/5/abcd-1234.jpg.
        return ResponseEntity.ok(service.removeAttachment(id, fileUrl, getCompanyId()));
    }
}
