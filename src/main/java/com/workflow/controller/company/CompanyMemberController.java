package com.workflow.controller.company;

import com.workflow.common.security.RequireCompanyRole;
import com.workflow.common.util.AuthUtils;
import com.workflow.dto.company.*;
import com.workflow.service.company.ICompanyMemberService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.workflow.common.constant.CompanyRole.*;

@Tag(name = "Company Members")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/companies/members")
public class CompanyMemberController {

    private final ICompanyMemberService memberService;

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER})
    @PostMapping("/invite")
    public ResponseEntity<MemberInviteResponse> inviteMember(
            @Valid @RequestBody MemberInviteRequest request
    ) {
        Long companyId = AuthUtils.getCompanyId();
        return ResponseEntity.ok(memberService.sendInvitation(
                request.email(),
                request.companyRole().name(),
                companyId
        ));
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER})
    @GetMapping("/invitations")
    public ResponseEntity<List<MemberInvitationStatusResponse>> listInvitations() {
        return ResponseEntity.ok(memberService.listInvitations(AuthUtils.getCompanyId()));
    }

    @GetMapping("/invitations/check")
    public ResponseEntity<MemberInvitationCheckResponse> checkInvitation(
            @RequestParam String token
    ) {
        return ResponseEntity.ok(memberService.checkInvitation(token));
    }

    @RequireCompanyRole({COMPANY_ADMIN, MANAGER, EDITOR, VIEWER})
    @GetMapping
    public ResponseEntity<List<MemberResponse>> listMembers() {
        return ResponseEntity.ok(memberService.listMembers(AuthUtils.getCompanyId()));
    }

    @RequireCompanyRole({COMPANY_ADMIN})
    @PutMapping("/{id}/role")
    public ResponseEntity<MemberResponse> changeMemberRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        Long companyId = AuthUtils.getCompanyId();
        return ResponseEntity.ok(memberService.changeMemberRole(id, body.get("companyRole"), companyId));
    }

    @RequireCompanyRole({COMPANY_ADMIN})
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeMember(@PathVariable Long id) {
        memberService.removeMember(id, AuthUtils.getCompanyId());
        return ResponseEntity.noContent().build();
    }
}
