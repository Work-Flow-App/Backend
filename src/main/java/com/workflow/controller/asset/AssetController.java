package com.workflow.controller.asset;

import com.workflow.dto.asset.*;
import com.workflow.entity.Company;
import com.workflow.entity.User;
import com.workflow.service.asset.IAssetService;
import com.workflow.service.company.ICompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDate;

@Tag(name = "Assets")
@RestController
@RequestMapping("/api/v1/assets")
@RequiredArgsConstructor
public class AssetController {

    private final IAssetService service;
    private final ICompanyService companyService;

    private Long getCompanyId(Authentication auth) {
        User user = (User) auth.getPrincipal();
        Company company = companyService.findCompanyByUserId(user.getId());
        return company.getId();
    }

    @PostMapping
    public ResponseEntity<AssetResponse> create(@RequestBody AssetCreateRequest request, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createAsset(request, getCompanyId(auth)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AssetResponse> update(@PathVariable Long id, @RequestBody AssetUpdateRequest request,
            Authentication auth) {
        return ResponseEntity.ok(service.updateAsset(id, request, getCompanyId(auth)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AssetResponse> get(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(service.getAsset(id, getCompanyId(auth)));
    }

    @GetMapping
    public ResponseEntity<Page<AssetResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean archived,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "asc") String dir,
            Authentication auth) {
        return ResponseEntity.ok(service.listAssets(getCompanyId(auth), page, size, archived, available, sort, dir));
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<Void> archive(@PathVariable Long id, Authentication auth) {
        service.archiveAsset(id, getCompanyId(auth));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/value")
    public ResponseEntity<AssetValueResponse> value(
            @PathVariable Long id,
            @RequestParam(required = false) String asOf, // ISO date yyyy-MM-dd
            Authentication auth) {
        LocalDate date = asOf == null ? null : LocalDate.parse(asOf);
        return ResponseEntity.ok(service.calculateAssetValue(id, getCompanyId(auth), date));
    }

    @GetMapping("/statistics")
    public ResponseEntity<Object> stats(Authentication auth) {
        return ResponseEntity.ok(service.getStatistics(getCompanyId(auth)));
    }
}
