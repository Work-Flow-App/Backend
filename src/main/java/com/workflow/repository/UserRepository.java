package com.workflow.repository;

import com.workflow.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface UserRepository extends JpaRepository<User, String> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByUuid (String uuid);

    Optional<User> findByUuid (String uuid);

    Optional<User> findByUsername (String userName);


}
