package com.workflow.repository.auth;

import com.workflow.entity.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.Set;


public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsByUuid (String uuid);

    Optional<User> findByUuid (String uuid);

    Optional<User> findByUsername (String userName);

    Optional<User> findByEmail (String email);

    Optional<User> findByGoogleId(String googleId);

    Set<User> findByUsernameIn(Set<String> usernames);

}
