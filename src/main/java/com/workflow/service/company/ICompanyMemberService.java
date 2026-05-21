package com.workflow.service.company;

import com.workflow.dto.company.*;

import java.util.List;

public interface ICompanyMemberService {

    MemberInviteResponse sendInvitation(String email, String companyRole, Long companyId);

    MemberSignupResponse validateAndAcceptInvitation(MemberSignupRequest request);

    MemberInvitationCheckResponse checkInvitation(String token);

    List<MemberInvitationStatusResponse> listInvitations(Long companyId);

    List<MemberResponse> listMembers(Long companyId);

    MemberResponse changeMemberRole(Long memberId, String newRole, Long companyId);

    void removeMember(Long memberId, Long companyId);
}
