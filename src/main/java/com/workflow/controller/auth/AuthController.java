package com.workflow.controller.auth;

import com.workflow.dto.auth.*;
import com.workflow.dto.auth.password.ForgotPasswordRequest;
import com.workflow.dto.auth.password.PasswordResetResponse;
import com.workflow.dto.auth.password.ResetPasswordRequest;
import com.workflow.dto.worker.WorkerSignupRequest;
import com.workflow.dto.worker.WorkerSignupResponse;
import com.workflow.entity.User;
import com.workflow.service.auth.AuthenticationService;
import com.workflow.service.auth.GoogleAuthService;
import com.workflow.service.auth.PasswordResetService;
import com.workflow.service.user.IUserService;
import com.workflow.service.worker.WorkerInvitationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Authentication")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final IUserService userService;
    private final AuthenticationService authService;
    private final GoogleAuthService googleAuthService;
    private final PasswordResetService passwordResetService;
    private final WorkerInvitationService workerInvitationService;

    @PostMapping("/google")
    public ResponseEntity<AuthenticationResponse> googleLogin(
            @Valid @RequestBody GoogleAuthRequest request,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(googleAuthService.authenticate(request, httpRequest));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(this.authService.authenticate(request, httpRequest));
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthenticationResponse> signup(
            @Valid @RequestBody SignupRequest request,
            HttpServletRequest httpRequest
    ) {
        User user = this.userService.createUser(request);
        return ResponseEntity.ok(this.authService.generateJwtToken(user, httpRequest));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(this.authService.refreshAccessToken(request, httpRequest));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Valid @RequestBody LogoutRequest request
    ) {
        this.authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutFromAllDevices(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        this.authService.logoutFromAllDevices(user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<PasswordResetResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        this.passwordResetService.createPasswordResetToken(request.email());
        return ResponseEntity.ok(
                new PasswordResetResponse("If the email exists, a verification code has been sent to it.")
        );
    }

    @PostMapping("/reset-password")
    public ResponseEntity<PasswordResetResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        this.passwordResetService.resetPassword(request.email(), request.code(), request.newPassword());
        return ResponseEntity.ok(
                new PasswordResetResponse("Password has been reset successfully. Please login with your new password.")
        );
    }

    @PostMapping("/signup/worker")
    public ResponseEntity<WorkerSignupResponse> signupWorker(
            @Valid @RequestBody WorkerSignupRequest request
    ) {
        WorkerSignupResponse response = this.workerInvitationService.validateAndAcceptInvitation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

