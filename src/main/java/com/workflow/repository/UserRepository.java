package com.workflow.repository;

import com.workflow.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsByUuid (String uuid);

    Optional<User> findByUuid (String uuid);

    Optional<User> findByUsername (String userName);

    Optional<User> findByEmail (String email);


}
