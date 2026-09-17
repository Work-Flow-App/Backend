package com.workflow.controller.company;

import com.workflow.dto.company.CompanyPostGroupRequest;
import com.workflow.dto.company.CompanyPostGroupResponse;
import com.workflow.service.company.ICompanyPostGroupService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Company Post Groups")
@RestController
@RequestMapping("/api/v1/companies/{companyId}/post-groups")
@RequiredArgsConstructor
public class CompanyPostGroupController {

    private final ICompanyPostGroupService groupService;

    @PostMapping
    public ResponseEntity<CompanyPostGroupResponse> createGroup(
            @PathVariable Long companyId,
            @Valid @RequestBody CompanyPostGroupRequest request) {
        return new ResponseEntity<>(groupService.createGroup(companyId, request), HttpStatus.CREATED);
    }

    @PutMapping("/{groupId}")
    public ResponseEntity<CompanyPostGroupResponse> updateGroup(
            @PathVariable Long companyId,
            @PathVariable Long groupId,
            @Valid @RequestBody CompanyPostGroupRequest request) {
        return ResponseEntity.ok(groupService.updateGroup(companyId, groupId, request));
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<Void> deleteGroup(
            @PathVariable Long companyId,
            @PathVariable Long groupId) {
        groupService.deleteGroup(companyId, groupId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<CompanyPostGroupResponse>> getAllGroups(@PathVariable Long companyId) {
        return ResponseEntity.ok(groupService.getAllGroups(companyId));
    }
}