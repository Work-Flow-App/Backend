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

    @Override
    @Transactional
    public User findOrCreateGoogleUser(String googleId, String email, String name) {
        // 1. Exact match by google_id
        return userRepository.findByGoogleId(googleId).orElseGet(() -> {
            // 2. Email match — link google_id to existing user
            return userRepository.findByEmail(email).map(existing -> {
                existing.setGoogleId(googleId);
                return userRepository.save(existing);
            }).orElseGet(() -> {
                // 3. Brand-new user
                String username = generateUniqueUsername(email, name);
                User user = User.builder()
                        .uuid(UUID.randomUUID().toString())
                        .username(username)
                        .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                        .email(email)
                        .googleId(googleId)
                        .role(Role.COMPANY)
                        .enabled(true)
                        .build();
                User saved = userRepository.save(user);
                createDefaultCompany(saved);
                return saved;
            });
        });
    }

    private String generateUniqueUsername(String email, String name) {
        // Derive a base username from name or email prefix
        String base = (name != null && !name.isBlank())
                ? name.toLowerCase().replaceAll("\\s+", "_").replaceAll("[^a-z0-9_]", "")
                : email.split("@")[0].toLowerCase().replaceAll("[^a-z0-9_]", "");
        if (base.isBlank()) base = "user";

        String candidate = base;
        int suffix = 1;
        while (userRepository.findByUsername(candidate).isPresent()) {
            candidate = base + suffix++;
        }
        return candidate;
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
