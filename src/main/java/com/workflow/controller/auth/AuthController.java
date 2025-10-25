package com.workflow.controller.auth;

import com.workflow.dto.auth.*;
import com.workflow.dto.password.ForgotPasswordRequest;
import com.workflow.dto.password.PasswordResetResponse;
import com.workflow.dto.password.ResetPasswordRequest;
import com.workflow.entity.User;
import com.workflow.service.auth.AuthenticationService;
import com.workflow.service.password.PasswordResetService;
import com.workflow.service.user.IUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final IUserService userService;
    private final AuthenticationService authService;
    private final PasswordResetService passwordResetService;

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
}

