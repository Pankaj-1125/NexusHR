package com.nexushr.repository;

import com.nexushr.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository
        extends JpaRepository<User, Long> {

    Optional<User> findByUsername(
            String username
    );

    Optional<User> findByUsernameIgnoreCase(
            String username
    );

    List<User> findAllByRecoveryEmailIgnoreCase(
            String recoveryEmail
    );
}