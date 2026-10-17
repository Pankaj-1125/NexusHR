package com.nexushr.service;

import com.nexushr.entity.Employee;
import com.nexushr.entity.PasswordResetToken;
import com.nexushr.entity.User;

import com.nexushr.repository.EmployeeRepository;
import com.nexushr.repository.PasswordResetTokenRepository;
import com.nexushr.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

import java.time.LocalDateTime;

import java.util.List;
import java.util.Locale;

@Service
public class PasswordResetService {

    private static final int OTP_EXPIRY_MINUTES =
            10;

    private final PasswordResetTokenRepository
            resetTokenRepository;

    private final UserRepository
            userRepository;

    private final EmployeeRepository
            employeeRepository;

    private final PasswordEncoder
            passwordEncoder;

    private final EmailNotificationService
            emailNotificationService;

    private final RefreshTokenService
            refreshTokenService;

    private final AuditLogService
            auditLogService;

    private final SecureRandom secureRandom =
            new SecureRandom();

    public PasswordResetService(
            PasswordResetTokenRepository resetTokenRepository,
            UserRepository userRepository,
            EmployeeRepository employeeRepository,
            PasswordEncoder passwordEncoder,
            EmailNotificationService emailNotificationService,
            RefreshTokenService refreshTokenService,
            AuditLogService auditLogService
    ) {

        this.resetTokenRepository =
                resetTokenRepository;

        this.userRepository =
                userRepository;

        this.employeeRepository =
                employeeRepository;

        this.passwordEncoder =
                passwordEncoder;

        this.emailNotificationService =
                emailNotificationService;

        this.refreshTokenService =
                refreshTokenService;

        this.auditLogService =
                auditLogService;
    }

    // =========================================================
    // REQUEST RESET OTP
    // =========================================================

    @Transactional
    public void requestReset(
            String usernameOrEmail
    ) {

        if (usernameOrEmail == null
                || usernameOrEmail.isBlank()) {

            return;
        }

        String value =
                usernameOrEmail
                        .trim();

        User user =
                resolveUser(
                        value
                );

        /*
         * Do not reveal whether account exists.
         */
        if (user == null
                || !user.isEnabled()) {

            return;
        }

        String recoveryEmail =
                resolveRecoveryEmail(
                        user
                );

        if (recoveryEmail == null
                || recoveryEmail.isBlank()) {

            return;
        }

        String otp =
                String.format(
                        "%06d",
                        secureRandom.nextInt(
                                1_000_000
                        )
                );

        String otpHash =
                passwordEncoder.encode(
                        otp
                );

        PasswordResetToken token =
                new PasswordResetToken(
                        user.getUsername(),
                        otpHash,
                        LocalDateTime
                                .now()
                                .plusMinutes(
                                        OTP_EXPIRY_MINUTES
                                )
                );

        resetTokenRepository.save(
                token
        );

        String message =
                "Your NexusHR password reset code is: "
                        + otp
                        + "\n\nThis code expires in "
                        + OTP_EXPIRY_MINUTES
                        + " minutes."
                        + "\n\nIf you did not request this reset, ignore this email.";

        emailNotificationService
                .sendToAddress(
                        recoveryEmail,
                        "NexusHR Password Reset",
                        message
                );

        auditLogService.log(
                user.getUsername(),
                "PASSWORD_RESET_REQUESTED",
                "AUTHENTICATION",
                user.getId(),
                "Password reset requested",
                null
        );
    }

    // =========================================================
    // COMPATIBILITY RESET METHOD
    // =========================================================

    @Transactional
    public void resetPassword(
            String username,
            String otp,
            String newPassword
    ) {

        resetPassword(
                username,
                otp,
                newPassword,
                null
        );
    }

    // =========================================================
    // RESET PASSWORD / FIRST-TIME USERNAME SETUP
    // =========================================================

    @Transactional
    public void resetPassword(
            String usernameOrEmail,
            String otp,
            String newPassword,
            String desiredUsername
    ) {

        if (usernameOrEmail == null
                || usernameOrEmail.isBlank()) {

            throw new RuntimeException(
                    "Username or email is required"
            );
        }

        if (otp == null
                || otp.isBlank()) {

            throw new RuntimeException(
                    "Reset code is required"
            );
        }

        validatePassword(
                newPassword
        );

        User user =
                resolveUser(
                        usernameOrEmail
                                .trim()
                );

        if (user == null
                || !user.isEnabled()) {

            throw new RuntimeException(
                    "Invalid or expired reset request"
            );
        }

        PasswordResetToken resetToken =
                resetTokenRepository
                        .findTopByUsernameAndUsedFalseOrderByCreatedAtDesc(
                                user.getUsername()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Invalid or expired reset code"
                                )
                        );

        if (resetToken
                .getExpiresAt()
                .isBefore(
                        LocalDateTime.now()
                )) {

            throw new RuntimeException(
                    "Reset code has expired"
            );
        }

        if (!passwordEncoder.matches(
                otp.trim(),
                resetToken.getCodeHash()
        )) {

            throw new RuntimeException(
                    "Invalid reset code"
            );
        }

        // =====================================================
        // USERNAME MAY BE SELECTED ONLY ONCE BY EMPLOYEE
        // =====================================================

        boolean firstTimeEmployeeSetup =
                "EMPLOYEE".equalsIgnoreCase(
                        user.getRole()
                )
                        &&
                        user.getUsername() != null
                        &&
                        user.getUsername()
                                .startsWith(
                                        "pending_"
                                );

        if (firstTimeEmployeeSetup) {

            if (desiredUsername == null
                    || desiredUsername.isBlank()) {

                throw new RuntimeException(
                        "Please choose your username"
                );
            }

            String chosenUsername =
                    desiredUsername
                            .trim()
                            .toLowerCase(
                                    Locale.ROOT
                            );

            if (!chosenUsername.matches(
                    "^[a-z0-9._-]{3,50}$"
            )) {

                throw new RuntimeException(
                        "Username must be 3-50 characters and may contain letters, numbers, dot, underscore or hyphen"
                );
            }

            userRepository
                    .findByUsername(
                            chosenUsername
                    )
                    .ifPresent(
                            existing -> {

                                if (!existing
                                        .getId()
                                        .equals(
                                                user.getId()
                                        )) {

                                    throw new RuntimeException(
                                            "Username already exists. Choose another username"
                                    );
                                }
                            }
                    );

            String oldPendingUsername =
                    user.getUsername();

            user.setUsername(
                    chosenUsername
            );

            /*
             * Token belongs to old pending username,
             * but after verification it will be marked used.
             */
            auditLogService.log(
                    chosenUsername,
                    "EMPLOYEE_USERNAME_CREATED",
                    "AUTHENTICATION",
                    user.getId(),
                    "Employee selected username during first-time setup",
                    null
            );

        } else if (
                desiredUsername != null
                        &&
                        !desiredUsername.isBlank()
        ) {

            /*
             * Real username already exists.
             * No account may change it through reset flow.
             */
            throw new RuntimeException(
                    "Username can only be selected once during first-time employee setup"
            );
        }

        // =====================================================
        // PASSWORD
        // =====================================================

        user.setPassword(
                passwordEncoder.encode(
                        newPassword
                )
        );

        userRepository.save(
                user
        );

        resetToken.setUsed(
                true
        );

        resetTokenRepository.save(
                resetToken
        );

        /*
         * Invalidate logged-in sessions after password change.
         */
        refreshTokenService
                .revokeAllForUser(
                        user.getUsername()
                );

        auditLogService.log(
                user.getUsername(),
                "PASSWORD_RESET_COMPLETED",
                "AUTHENTICATION",
                user.getId(),
                "Password changed using reset code",
                null
        );
    }

    // =========================================================
    // USER RESOLUTION
    // =========================================================

    private User resolveUser(
            String value
    ) {

        if (value == null
                || value.isBlank()) {

            return null;
        }

        /*
         * 1. Username.
         */
        User user =
                userRepository
                        .findByUsername(
                                value
                        )
                        .orElse(
                                null
                        );

        if (user != null) {

            return user;
        }

        /*
         * 2. Employee email has priority.
         *
         * Important because Admin and Employee might
         * share the same recovery email.
         */
        user =
                employeeRepository
                        .findByEmail(
                                value
                        )
                        .map(
                                Employee::getUser
                        )
                        .orElse(
                                null
                        );

        if (user != null) {

            return user;
        }

        /*
         * 3. Generic recovery email only when exactly
         * one account matches.
         */
        List<User> matches =
                userRepository
                        .findAllByRecoveryEmailIgnoreCase(
                                value
                        );

        if (matches.size() == 1) {

            return matches.get(
                    0
            );
        }

        return null;
    }

    // =========================================================
    // RECOVERY EMAIL
    // =========================================================

    private String resolveRecoveryEmail(
            User user
    ) {

        if (user == null) {

            return null;
        }

        if (user.getRecoveryEmail() != null
                &&
                !user.getRecoveryEmail()
                        .isBlank()) {

            return user
                    .getRecoveryEmail()
                    .trim();
        }

        return employeeRepository
                .findByUserUsername(
                        user.getUsername()
                )
                .map(
                        Employee::getEmail
                )
                .orElse(
                        null
                );
    }

    // =========================================================
    // PASSWORD VALIDATION
    // =========================================================

    private void validatePassword(
            String password
    ) {

        if (password == null
                || password.length() < 8) {

            throw new RuntimeException(
                    "Password must contain at least 8 characters"
            );
        }

        if (!password.matches(
                ".*[A-Z].*"
        )) {

            throw new RuntimeException(
                    "Password must contain an uppercase letter"
            );
        }

        if (!password.matches(
                ".*[a-z].*"
        )) {

            throw new RuntimeException(
                    "Password must contain a lowercase letter"
            );
        }

        if (!password.matches(
                ".*\\d.*"
        )) {

            throw new RuntimeException(
                    "Password must contain a number"
            );
        }
    }
}