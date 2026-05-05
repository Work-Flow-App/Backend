package com.workflow.service.user;

import com.workflow.dto.auth.SignupRequest;
import com.workflow.dto.auth.UserLookupResult;
import com.workflow.entity.auth.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;


public interface IUserService extends UserDetailsService {

    User createUser(SignupRequest request);
    UserLookupResult findOrCreateGoogleUser(String googleId, String email, String name);
    void enableUser(User user);
    void deactivateAccount(String uuid);
    void reactivateAccount(String uuid);
    void deleteAccount(String uuid);
}
