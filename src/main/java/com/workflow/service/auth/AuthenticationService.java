package com.workflow.service.auth;

import com.workflow.dto.auth.AuthenticationResponse;
import com.workflow.dto.auth.LoginRequest;
import com.workflow.repository.UserRepository;
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


    public AuthenticationResponse authenticate(LoginRequest request) {
        // Spring Security will throw if invalid
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.userName(),
                        request.password()
                )
        );

        var user = userRepository.findByUsername(request.userName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + request.userName()));

        var jwtToken = jwtService.generateToken(user);

        return AuthenticationResponse.success(jwtToken);
    }

    public AuthenticationResponse generateJwtToken(UserDetails user) {
        var jwtToken = jwtService.generateToken(user);

        return AuthenticationResponse.success(jwtToken);
    }


}

