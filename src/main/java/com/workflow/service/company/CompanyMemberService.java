package com.workflow.service.company;

import com.workflow.common.constant.CompanyRole;
import com.workflow.common.constant.InvitationStatus;
import com.workflow.common.constant.Role;
import com.workflow.common.exception.business.*;
import com.workflow.dto.company.*;
import com.workflow.entity.auth.User;
import com.workflow.entity.company.Company;
import com.workflow.entity.company.CompanyMember;
import com.workflow.entity.company.CompanyMemberInvitation;
import com.workflow.repository.auth.UserRepository;
import com.workflow.repository.company.CompanyMemberInvitationRepository;
import com.workflow.repository.company.CompanyMemberRepository;
import com.workflow.repository.company.CompanyRepository;
import com.workflow.service.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CompanyMemberService implements ICompanyMemberService {

    private final CompanyMemberInvitationRepository invitationRepository;
    private final CompanyMemberRepository memberRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${member-invitation.token.expiration-days}")
    private int expirationDays;

    @Override
    public MemberInviteResponse sendInvitation(String email, String companyRoleStr, Long companyId) {
        CompanyRole companyRole = parseRole(companyRoleStr);

        if (companyRole == CompanyRole.COMPANY_ADMIN) {
            throw new InvalidRequestException("Cannot invite users with COMPANY_ADMIN role");
        }

        Company company = findActiveCompany(companyId);

        User existingUser = userRepository.findByEmail(email).orElse(null);
        if (existingUser != null) {
            CompanyMember inactiveMember = memberRepository
                    .findInactiveByCompanyIdAndUserId(companyId, existingUser.getId())
                    .orElseThrow(() -> new UserAlreadyExistsException("Email already registered"));

            inactiveMember.setActive(true);
            inactiveMember.setCompanyRole(companyRole);
            memberRepository.save(inactiveMember);

            emailService.sendMemberReactivationEmail(email, company.getName(), companyRole);

            log.info("Member reactivated: userId={}, company={}, role={}", existingUser.getId(), company.getName(), companyRole);

            return new MemberInviteResponse(email, companyRole, "Member reactivated successfully", null);
        }

        invitationRepository.invalidatePreviousInvitations(email, companyId);

        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now(ZoneOffset.UTC).plusDays(expirationDays);

        CompanyMemberInvitation invitation = CompanyMemberInvitation.builder()
                .invitationToken(token)
                .email(email)
                .company(company)
                .companyRole(companyRole)
                .expiresAt(expiresAt)
                .used(false)
                .build();

        invitationRepository.save(invitation);
        emailService.sendCompanyMemberInvitationEmail(email, company.getName(), companyRole, token);

        log.info("Member invitation sent: email={}, company={}, role={}", email, company.getName(), companyRole);

        return new MemberInviteResponse(email, companyRole, "Invitation sent successfully", expiresAt);
    }

    @Override
    public MemberSignupResponse validateAndAcceptInvitation(MemberSignupRequest request) {
        CompanyMemberInvitation invitation = invitationRepository.findByInvitationToken(request.invitationToken())
                .orElseThrow(() -> new InvalidRequestException("Invalid or expired invitation token"));

        if (!invitation.isValid()) {
            if (invitation.isUsed()) {
                throw new InvalidRequestException("Invitation has already been used");
            }
            throw new InvalidRequestException("Invitation has expired");
        }

        if (!request.email().equalsIgnoreCase(invitation.getEmail())) {
            throw new InvalidRequestException("Email does not match invitation");
        }

        Company company = invitation.getCompany();
        if (company.isArchived()) {
            throw new CompanyNotFoundException("Company not found");
        }

        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new UserAlreadyExistsException("Username '" + request.username() + "' is already taken");
        }

        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new UserAlreadyExistsException("Email already registered");
        }

        invitation.markAsUsed();
        invitationRepository.save(invitation);

        User user = User.builder()
                .uuid(UUID.randomUUID().toString())
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.COMPANY)
                .enabled(true)
                .build();
        userRepository.save(user);

        CompanyMember member = CompanyMember.builder()
                .company(company)
                .user(user)
                .name(request.name())
                .companyRole(invitation.getCompanyRole())
                .active(true)
                .build();
        memberRepository.save(member);

        log.info("Member signup accepted: memberId={}, email={}, company={}, role={}",
                member.getId(), user.getEmail(), company.getName(), member.getCompanyRole());

        return new MemberSignupResponse(
                member.getId(),
                user.getUsername(),
                user.getEmail(),
                company.getName(),
                member.getCompanyRole(),
                "Account created successfully"
        );
    }

    @Override
    @Transactional(readOnly = true)
    public MemberInvitationCheckResponse checkInvitation(String token) {
        CompanyMemberInvitation invitation = invitationRepository.findByInvitationToken(token)
                .orElseThrow(() -> new InvalidRequestException("Invalid invitation token"));

        InvitationStatus status;
        boolean valid;
        if (invitation.isUsed()) {
            status = InvitationStatus.ACCEPTED;
            valid = false;
        } else if (invitation.isExpired()) {
            status = InvitationStatus.EXPIRED;
            valid = false;
        } else {
            status = InvitationStatus.PENDING;
            valid = true;
        }

        if (invitation.getCompany().isArchived()) {
            throw new CompanyNotFoundException("Company not found");
        }

        return new MemberInvitationCheckResponse(
                valid,
                invitation.getEmail(),
                invitation.getCompany().getName(),
                invitation.getCompanyRole(),
                status,
                invitation.getExpiresAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberInvitationStatusResponse> listInvitations(Long companyId) {
        return invitationRepository.findByCompanyIdOrderByCreatedAtDesc(companyId).stream()
                .map(this::toInvitationStatusResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberResponse> listMembers(Long companyId) {
        return memberRepository.findActiveByCompanyId(companyId).stream()
                .map(this::toMemberResponse)
                .collect(Collectors.toList());
    }

    @Override
    public MemberResponse changeMemberRole(Long memberId, String newRoleStr, Long companyId) {
        CompanyRole newRole = parseRole(newRoleStr);

        if (newRole == CompanyRole.COMPANY_ADMIN) {
            throw new InvalidRequestException("Cannot assign COMPANY_ADMIN role");
        }

        CompanyMember member = findMember(memberId, companyId);

        if (member.getCompanyRole() == CompanyRole.COMPANY_ADMIN) {
            throw new ForbiddenActionException("Cannot change role of COMPANY_ADMIN");
        }

        member.setCompanyRole(newRole);
        memberRepository.save(member);

        log.info("Member role changed: memberId={}, newRole={}, companyId={}", memberId, newRole, companyId);
        return toMemberResponse(member);
    }

    @Override
    public void removeMember(Long memberId, Long companyId) {
        CompanyMember member = findMember(memberId, companyId);

        if (member.getCompanyRole() == CompanyRole.COMPANY_ADMIN) {
            throw new ForbiddenActionException("Cannot remove COMPANY_ADMIN");
        }

        member.setActive(false);
        memberRepository.save(member);

        log.info("Member removed: memberId={}, companyId={}", memberId, companyId);
    }

    private CompanyMember findMember(Long memberId, Long companyId) {
        CompanyMember member = memberRepository.findById(memberId)
                .orElseThrow(() -> new UserNotFoundException("Member not found"));
        if (!member.getCompany().getId().equals(companyId)) {
            throw new ForbiddenActionException("Member does not belong to your company");
        }
        return member;
    }

    private Company findActiveCompany(Long companyId) {
        return companyRepository.findByIdAndNotArchived(companyId)
                .orElseThrow(() -> new CompanyNotFoundException("Company not found"));
    }

    private CompanyRole parseRole(String role) {
        try {
            return CompanyRole.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Invalid company role: " + role);
        }
    }

    private MemberResponse toMemberResponse(CompanyMember member) {
        User user = member.getUser();
        return new MemberResponse(
                member.getId(),
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                member.getName(),
                member.getCompanyRole(),
                member.isActive(),
                member.getCreatedAt()
        );
    }

    private MemberInvitationStatusResponse toInvitationStatusResponse(CompanyMemberInvitation invitation) {
        InvitationStatus status;
        if (invitation.isUsed()) {
            status = InvitationStatus.ACCEPTED;
        } else if (invitation.isExpired()) {
            status = InvitationStatus.EXPIRED;
        } else {
            status = InvitationStatus.PENDING;
        }
        return new MemberInvitationStatusResponse(
                invitation.getId(),
                invitation.getEmail(),
                invitation.getCompanyRole(),
                status,
                invitation.getCreatedAt(),
                invitation.getExpiresAt(),
                invitation.getUsedAt()
        );
    }
}
