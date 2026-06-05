package com.workflow.service.worker;

import com.workflow.common.constant.InvitationStatus;
import com.workflow.common.constant.Role;
import com.workflow.common.exception.business.*;
import com.workflow.dto.worker.*;
import com.workflow.entity.auth.User;
import com.workflow.entity.company.Company;
import com.workflow.entity.worker.Worker;
import com.workflow.entity.worker.WorkerInvitation;
import com.workflow.repository.auth.UserRepository;
import com.workflow.repository.worker.WorkerInvitationRepository;
import com.workflow.repository.worker.WorkerRepository;
import com.workflow.service.company.ICompanyService;
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
public class WorkerInvitationService {

    private final WorkerInvitationRepository invitationRepository;
    private final WorkerRepository workerRepository;
    private final UserRepository userRepository;
    private final ICompanyService companyService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${worker-invitation.token.expiration-days}")
    private int expirationDays;

    /**
     * Create and send a worker invitation
     */
    @Transactional
    public WorkerInviteResponse createInvitation(String email, Long companyUserId) {
        log.info("Creating worker invitation for email: {}", email);

        // Get company
        Company company = companyService.findCompanyByUserId(companyUserId);

        // Check if email already registered in User table
        if (userRepository.findByEmail(email).isPresent()) {
            log.warn("Attempted to invite already registered email: {}", email);
            throw new UserAlreadyExistsException("Email already registered");
        }

        // Check if worker with this email already exists
        if (workerRepository.existsByEmailIgnoreCaseAndArchivedFalse(email)) {
            log.warn("Attempted to invite existing worker email: {}", email);
            throw new WorkerAlreadyExistsException("Worker with this email already exists");
        }

        // Invalidate any previous invitations for this email/company
        invitationRepository.invalidatePreviousInvitations(email, company.getId());

        // Generate UUID token
        String token = UUID.randomUUID().toString();

        // Create invitation with expiration
        LocalDateTime expiresAt = LocalDateTime.now(ZoneOffset.UTC).plusDays(expirationDays);
        WorkerInvitation invitation = WorkerInvitation.builder()
                .invitationToken(token)
                .email(email)
                .company(company)
                .expiresAt(expiresAt)
                .used(false)
                .build();

        invitationRepository.save(invitation);

        // Send invitation email
        emailService.sendWorkerInvitationEmail(email, company.getName(), token);

        log.info("Worker invitation created successfully for email: {}, company: {}, expiresAt: {}",
                email, company.getName(), expiresAt);

        return new WorkerInviteResponse(
                email,
                "Invitation sent successfully",
                expiresAt
        );
    }

    /**
     * Validate invitation and create worker account
     */
    @Transactional
    public WorkerSignupResponse validateAndAcceptInvitation(WorkerSignupRequest request) {
        log.info("Validating invitation token for email: {}", request.email());

        // Find invitation by token
        WorkerInvitation invitation = invitationRepository.findByInvitationToken(request.invitationToken())
                .orElseThrow(() -> {
                    log.error("Invalid invitation token attempted");
                    return new InvalidWorkerInvitationException("Invalid or expired invitation token");
                });

        // Validate invitation is still valid (not used, not expired)
        if (!invitation.isValid()) {
            if (invitation.isUsed()) {
                log.warn("Attempted to use already used invitation: {}", invitation.getId());
                throw new InvalidWorkerInvitationException("Invitation has already been used");
            } else {
                log.warn("Attempted to use expired invitation: {}", invitation.getId());
                throw new InvalidWorkerInvitationException("Invalid or expired invitation token");
            }
        }

        // Verify email matches invitation email (case-insensitive)
        if (!request.email().equalsIgnoreCase(invitation.getEmail())) {
            log.error("Email mismatch - invitation: {}, request: {}", invitation.getEmail(), request.email());
            throw new InvalidWorkerInvitationException("Email does not match invitation");
        }

        // Verify company still exists and not archived
        Company company = invitation.getCompany();
        if (company.isArchived()) {
            log.error("Attempted signup with invitation from archived company: {}", company.getId());
            throw new CompanyNotFoundException("Company not found");
        }

        // Check username uniqueness
        if (userRepository.findByUsername(request.username()).isPresent()) {
            log.warn("Username already taken: {}", request.username());
            throw new UserAlreadyExistsException("Username '" + request.username() + "' is already taken");
        }

        // Double-check worker doesn't already exist
        if (workerRepository.existsByEmailIgnoreCaseAndArchivedFalse(request.email())) {
            log.error("Worker already exists with email: {}", request.email());
            throw new WorkerAlreadyExistsException("Worker with email '" + request.email() + "' already exists");
        }

        // Mark invitation as used BEFORE creating worker (prevent race conditions)
        invitation.markAsUsed();
        invitationRepository.save(invitation);

        // Create User account with WORKER role
        User user = User.builder()
                .uuid(UUID.randomUUID().toString())
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.WORKER)
                .enabled(true)
                .build();
        userRepository.save(user);

        // Create Worker entity linked to company
        Worker worker = Worker.builder()
                .name(request.name())
                .email(request.email())
                .company(company)
                .user(user)
                .loginLocked(false)
                .archived(false)
                .build();
        workerRepository.save(worker);

        log.info("Worker account created successfully: workerId={}, email={}, companyId={}, username={}",
                worker.getId(), worker.getEmail(), company.getId(), user.getUsername());

        return new WorkerSignupResponse(
                worker.getId(),
                worker.getName(),
                worker.getEmail(),
                user.getUsername(),
                company.getName(),
                "Account created successfully"
        );
    }

    /**
     * Check invitation token and return details (public endpoint for frontend)
     */
    @Transactional(readOnly = true)
    public WorkerInvitationCheckResponse checkInvitation(String token) {
        log.debug("Checking invitation token");

        // Find invitation by token
        WorkerInvitation invitation = invitationRepository.findByInvitationToken(token)
                .orElseThrow(() -> {
                    log.warn("Invalid invitation token attempted");
                    return new InvalidWorkerInvitationException("Invalid invitation token");
                });

        // Determine status
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

        // Check if company is archived
        if (invitation.getCompany().isArchived()) {
            log.warn("Invitation belongs to archived company: {}", invitation.getCompany().getId());
            throw new CompanyNotFoundException("Company not found");
        }

        log.debug("Invitation check successful - email: {}, status: {}, valid: {}",
                invitation.getEmail(), status, valid);

        return new WorkerInvitationCheckResponse(
                valid,
                invitation.getEmail(),
                invitation.getCompany().getName(),
                status,
                invitation.getExpiresAt()
        );
    }

    /**
     * Get all invitations for a company with status
     */
    @Transactional(readOnly = true)
    public List<WorkerInvitationStatusResponse> getInvitationsByCompany(Long companyUserId) {
        log.debug("Fetching invitations for company user: {}", companyUserId);

        // Get company
        Company company = companyService.findCompanyByUserId(companyUserId);

        // Fetch all invitations for this company
        List<WorkerInvitation> invitations = invitationRepository
                .findByCompanyIdOrderByCreatedAtDesc(company.getId());

        log.debug("Retrieved {} invitations for company: {}", invitations.size(), company.getName());

        // Map to response with status calculation
        return invitations.stream()
                .map(this::mapToStatusResponse)
                .collect(Collectors.toList());
    }

    /**
     * Map invitation to status response
     */
    private WorkerInvitationStatusResponse mapToStatusResponse(WorkerInvitation invitation) {
        InvitationStatus status;
        if (invitation.isUsed()) {
            status = InvitationStatus.ACCEPTED;
        } else if (invitation.isExpired()) {
            status = InvitationStatus.EXPIRED;
        } else {
            status = InvitationStatus.PENDING;
        }

        return new WorkerInvitationStatusResponse(
                invitation.getId(),
                invitation.getEmail(),
                status,
                invitation.getCreatedAt(),
                invitation.getExpiresAt(),
                invitation.getUsedAt()
        );
    }
}
