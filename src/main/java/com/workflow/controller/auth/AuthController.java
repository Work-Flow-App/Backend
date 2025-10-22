package com.workflow.controller.auth;

import com.workflow.dto.auth.AuthenticationResponse;
import com.workflow.dto.auth.LoginRequest;
import com.workflow.dto.auth.SignupRequest;
import com.workflow.entity.User;
import com.workflow.service.auth.AuthenticationService;
import com.workflow.service.user.IUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final IUserService userService;
    private final AuthenticationService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login (
            @Valid @RequestBody LoginRequest request
    ) {
        return ResponseEntity.ok(this.authService.authenticate(request));
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthenticationResponse> signup(
            @Valid @RequestBody SignupRequest request
    ) {
        User user = this.userService.createUser(request);
        return ResponseEntity.ok(this.authService.generateJwtToken(user));
    }
}
