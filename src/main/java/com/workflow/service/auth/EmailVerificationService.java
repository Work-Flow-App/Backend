package com.workflow.service.auth;

import com.workflow.common.exception.business.InvalidEmailVerificationTokenException;
import com.workflow.dto.auth.AuthenticationResponse;
import com.workflow.entity.auth.EmailVerificationToken;
import com.workflow.entity.auth.User;
import com.workflow.repository.auth.EmailVerificationTokenRepository;
import com.workflow.repository.auth.UserRepository;
import com.workflow.service.email.EmailService;
import com.workflow.service.firstpromoter.AffiliateTrackingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final AuthenticationService authenticationService;
    private final AffiliateTrackingService affiliateTrackingService;

    @Value("${email-verification.token.expiration-hours}")
    private int expirationHours;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Transactional
    public void sendVerificationEmail(User user) {
        // Invalidate any existing tokens for this user
        tokenRepository.deleteAllByUser(user);

        String token = UUID.randomUUID().toString();

        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .token(token)
                .user(user)
                .expiresAt(LocalDateTime.now().plusHours(expirationHours))
                .build();

        tokenRepository.save(verificationToken);

        String verificationLink = frontendUrl + "/verify-email?token=" + token;
        emailService.sendEmailVerificationEmail(user.getEmail(), user.getUsername(), verificationLink, expirationHours);

        log.info("Verification email sent to user: {}", user.getUsername());
    }

    @Transactional
    public AuthenticationResponse verifyEmail(String token, String tid, HttpServletRequest httpRequest) {
        EmailVerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidEmailVerificationTokenException("Invalid verification token"));

        if (verificationToken.isExpired()) {
            throw new InvalidEmailVerificationTokenException("Verification token has expired. Please request a new one.");
        }
        if (verificationToken.isUsed()) {
            throw new InvalidEmailVerificationTokenException("Verification token has already been used");
        }

        User user = verificationToken.getUser();
        user.setEnabled(true);
        userRepository.save(user);

        verificationToken.markAsUsed();
        tokenRepository.save(verificationToken);

        log.info("Email verified successfully for user: {}", user.getUsername());

        String capturedEmail = user.getEmail();
        String capturedTid = tid;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    affiliateTrackingService.trackSignup(capturedEmail, capturedTid);
                }
            });
        }

        return authenticationService.generateJwtToken(user, httpRequest);
    }

    @Transactional
    public void resendVerification(String email) {
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null || user.isEnabled()) {
            // Don't reveal whether email exists or is already verified
            log.info("Resend verification requested for email: {} (user not found or already verified)", email);
            return;
        }

        sendVerificationEmail(user);
    }
}
