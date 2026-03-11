package com.workflow.service.user;

import com.workflow.dto.auth.SignupRequest;
import com.workflow.entity.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;


public interface IUserService extends UserDetailsService {

    User createUser(SignupRequest request);
    User findOrCreateGoogleUser(String googleId, String email, String name);
    void deactivateAccount(String uuid);
    void reactivateAccount(String uuid);
    void deleteAccount(String uuid);
}
