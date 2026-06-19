package com.workflow.service.auth;

import com.workflow.config.properties.JwtConfigProperties;
import com.workflow.dto.auth.AuthenticationResponse;
import com.workflow.dto.auth.LoginRequest;
import com.workflow.dto.auth.RefreshTokenRequest;
import com.workflow.entity.auth.RefreshToken;
import com.workflow.entity.auth.User;
import com.workflow.repository.auth.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final JwtConfigProperties jwtConfigProperties;

    public AuthenticationResponse authenticate(LoginRequest request, HttpServletRequest httpRequest) {
        // Spring Security will throw if invalid
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.userName(),
                        request.password()
                )
        );

        User user = userRepository.findByUsername(request.userName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + request.userName()));

        String accessToken = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user, httpRequest);
        long expiresIn = jwtConfigProperties.getAccessToken().getExpirationMinutes() * 60L;

        return AuthenticationResponse.success(accessToken, refreshToken.getToken(), expiresIn);
    }

    public AuthenticationResponse generateJwtToken(UserDetails user, HttpServletRequest httpRequest) {
        String accessToken = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken((User) user, httpRequest);
        long expiresIn = jwtConfigProperties.getAccessToken().getExpirationMinutes() * 60L;

        return AuthenticationResponse.success(accessToken, refreshToken.getToken(), expiresIn);
    }

    public AuthenticationResponse refreshAccessToken(RefreshTokenRequest request, HttpServletRequest httpRequest) {
        // Validate refresh token
        RefreshToken refreshToken = refreshTokenService.validateRefreshToken(request.refreshToken());

        // Generate new access token
        String newAccessToken = jwtService.generateToken(refreshToken.getUser());

        // Rotate refresh token (security best practice)
        RefreshToken newRefreshToken = refreshTokenService.rotateRefreshToken(refreshToken, httpRequest);
        long expiresIn = jwtConfigProperties.getAccessToken().getExpirationMinutes() * 60L;

        return AuthenticationResponse.success(newAccessToken, newRefreshToken.getToken(), expiresIn);
    }

    public void logout(String refreshToken, User user) {
        refreshTokenService.revokeRefreshToken(refreshToken, user);
    }

    public void logoutFromAllDevices(User user) {
        refreshTokenService.revokeAllUserTokens(user);
    }
}

