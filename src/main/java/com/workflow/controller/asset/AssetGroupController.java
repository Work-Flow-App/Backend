package com.workflow.controller.asset;

import com.workflow.common.security.RequireCompanyRole;
import com.workflow.common.util.AuthUtils;
import com.workflow.dto.asset.AssetGroupCreateRequest;
import com.workflow.dto.asset.AssetGroupResponse;
import com.workflow.service.asset.IAssetGroupService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.workflow.common.constant.CompanyRole.*;

@Tag(name = "Asset Groups")
@RestController
@RequestMapping("/api/v1/asset-groups")
@RequiredArgsConstructor
public class AssetGroupController {

    private final IAssetGroupService service;

    private Long getCompanyId() {
        return AuthUtils.getCompanyId();
    }

    @RequireCompanyRole({ COMPANY_ADMIN, MANAGER, EDITOR })
    @PostMapping
    public ResponseEntity<AssetGroupResponse> create(@Valid @RequestBody AssetGroupCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createGroup(request, getCompanyId()));
    }

    @RequireCompanyRole({ COMPANY_ADMIN, MANAGER, EDITOR })
    @PutMapping("/{id}")
    public ResponseEntity<AssetGroupResponse> update(@PathVariable Long id,
            @Valid @RequestBody AssetGroupCreateRequest request) {
        return ResponseEntity.ok(service.updateGroup(id, request, getCompanyId()));
    }

    @RequireCompanyRole({ COMPANY_ADMIN, MANAGER, EDITOR })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteGroup(id, getCompanyId());
        return ResponseEntity.noContent().build();
    }

    @RequireCompanyRole({ COMPANY_ADMIN, MANAGER, EDITOR, VIEWER })
    @GetMapping
    public ResponseEntity<Page<AssetGroupResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return ResponseEntity.ok(service.listGroups(getCompanyId(), page, size));
    }
}