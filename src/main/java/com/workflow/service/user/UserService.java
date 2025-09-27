package com.workflow.service.user;

import com.workflow.common.exception.customException.UserAlreadyExistsException;
import com.workflow.dto.auth.SignupRequest;
import com.workflow.entity.User;
import com.workflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@RequiredArgsConstructor
@Service
public class UserService implements IUserService{

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    @Override
    public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException {
        return this.userRepository.findByUsername(userName)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username : " +userName));
    }

    @Override
    public User createUser(SignupRequest request) {
        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new UserAlreadyExistsException("User already exists with username: " + request.username());
        }

        User user = User.builder()
                .uuid(UUID.randomUUID().toString())
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .email(request.email())
                .role(request.role())
                .enabled(true)
                .build();

        return userRepository.save(user);
    }

//    @Override
//    public void updateProfileInfo(ProfileUpdateRequest request, String uuid) {
//        final User user = this.userRepository.findByUuid(uuid)
//                .orElseThrow(() ->new ResourceNotFoundException("User not found : " + request.email()));
//    }
//
//    @Override
//    public void changePassword(ChangePasswordRequest request, String uuid) {
//    }

    @Override
    public void deactivateAccount(String uuid) {

    }

    @Override
    public void reactivateAccount(String uuid) {

    }

    @Override
    public void deleteAccount(String uuid) {

    }
}
