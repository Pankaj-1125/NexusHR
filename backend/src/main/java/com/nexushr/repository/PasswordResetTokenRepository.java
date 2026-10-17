package com.nexushr.repository;

import com.nexushr.entity.PasswordResetToken;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository
        extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken>
    findTopByUsernameAndUsedFalseOrderByCreatedAtDesc(
            String username
    );

    void deleteAllByUsername(
            String username
    );
}