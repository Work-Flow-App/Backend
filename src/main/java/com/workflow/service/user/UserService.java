package com.workflow.service.user;

import com.workflow.common.constant.Role;
import com.workflow.common.exception.business.UserAlreadyExistsException;
import com.workflow.dto.auth.SignupRequest;
import com.workflow.entity.Company;
import com.workflow.entity.User;
import com.workflow.repository.CompanyRepository;
import com.workflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@RequiredArgsConstructor
@Service
public class UserService implements IUserService{

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;


    @Override
    public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException {
        return this.userRepository.findByUsername(userName)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username : " +userName));
    }

    @Override
    @Transactional
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

        User savedUser = userRepository.save(user);

        // If user is signing up as COMPANY, create a Company record automatically
        if (request.role() == Role.COMPANY) {
            createDefaultCompany(savedUser);
        }

        return savedUser;
    }

    private void createDefaultCompany(User user) {
        // Create company with minimal required information
        Company company = Company.builder()
                .name(user.getUsername() + "'s Company")  // Default company name
                .user(user)
                .email(user.getEmail())  // Use user's email
                .archived(false)
                .build();

        companyRepository.save(company);
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
