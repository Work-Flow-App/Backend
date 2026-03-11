package com.workflow.service.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.workflow.common.exception.business.InvalidGoogleTokenException;
import com.workflow.dto.auth.AuthenticationResponse;
import com.workflow.dto.auth.GoogleAuthRequest;
import com.workflow.entity.User;
import com.workflow.service.user.IUserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;

@RequiredArgsConstructor
@Service
public class GoogleAuthService {

    @Value("${google.client-id}")
    private String googleClientId;

    private final IUserService userService;
    private final AuthenticationService authenticationService;

    public AuthenticationResponse authenticate(GoogleAuthRequest request, HttpServletRequest httpRequest) {
        GoogleIdToken.Payload payload = verifyToken(request.idToken());

        String googleId = payload.getSubject();
        String email = payload.getEmail();
        String name = (String) payload.get("name");

        User user = userService.findOrCreateGoogleUser(googleId, email, name);
        return authenticationService.generateJwtToken(user, httpRequest);
    }

    private GoogleIdToken.Payload verifyToken(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), GsonFactory.getDefaultInstance()
            )
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new InvalidGoogleTokenException("Invalid Google ID token");
            }
            return idToken.getPayload();
        } catch (InvalidGoogleTokenException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidGoogleTokenException("Google token verification failed");
        }
    }
}
