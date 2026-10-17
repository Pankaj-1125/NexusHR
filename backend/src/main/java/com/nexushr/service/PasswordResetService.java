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

    private static final int OTP_EXPIRY_MINUTES = 10;

    private final PasswordResetTokenRepository resetTokenRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailNotificationService emailNotificationService;
    private final RefreshTokenService refreshTokenService;
    private final AuditLogService auditLogService;

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
    // REQUEST PASSWORD RESET OTP
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
         * Security:
         * Never reveal whether the account exists.
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


        // =====================================================
        // GENERATE OTP
        // =====================================================

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


        /*
         * Save temporarily.
         *
         * IMPORTANT:
         * If email delivery fails,
         * this token will immediately be removed.
         */
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


        // =====================================================
        // SEND OTP EMAIL
        // =====================================================

        EmailNotificationService.DeliveryResult deliveryResult =
                emailNotificationService
                        .sendToAddress(
                                recoveryEmail,
                                "NexusHR Password Reset",
                                message
                        );


        System.out.println(
                "[PASSWORD RESET EMAIL] Status="
                        + deliveryResult.status()
                        + " Message="
                        + deliveryResult.message()
        );


        // =====================================================
        // EMAIL DELIVERY FAILED
        // =====================================================

        if (!"SENT".equalsIgnoreCase(
                deliveryResult.status()
        )) {

            /*
             * CRITICAL FIX:
             *
             * Previously a failed email still left
             * the newly generated OTP in the database.
             *
             * That meant an older OTP delivered successfully
             * by email immediately became invalid.
             *
             * Delete the new token when delivery fails.
             */
            resetTokenRepository.delete(
                    token
            );


            System.err.println(
                    "[PASSWORD RESET EMAIL FAILED] "
                            + deliveryResult.message()
            );


            auditLogService.log(
                    user.getUsername(),
                    "PASSWORD_RESET_EMAIL_FAILED",
                    "AUTHENTICATION",
                    user.getId(),
                    "Password reset email could not be delivered",
                    null
            );


            return;
        }


        // =====================================================
        // EMAIL SUCCESS
        // =====================================================

        auditLogService.log(
                user.getUsername(),
                "PASSWORD_RESET_REQUESTED",
                "AUTHENTICATION",
                user.getId(),
                "Password reset code sent successfully",
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
    // RESET PASSWORD
    // FIRST-TIME USERNAME SETUP
    // =========================================================

    @Transactional
    public void resetPassword(
            String usernameOrEmail,
            String otp,
            String newPassword,
            String desiredUsername
    ) {

        // =====================================================
        // BASIC VALIDATION
        // =====================================================

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


        // =====================================================
        // FIND USER
        // =====================================================

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


        // =====================================================
        // GET LATEST ACTIVE OTP
        // =====================================================

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


        // =====================================================
        // OTP EXPIRY
        // =====================================================

        if (resetToken
                .getExpiresAt()
                .isBefore(
                        LocalDateTime.now()
                )) {

            throw new RuntimeException(
                    "Reset code has expired"
            );
        }


        // =====================================================
        // OTP MATCH
        // =====================================================

        if (!passwordEncoder.matches(
                otp.trim(),
                resetToken.getCodeHash()
        )) {

            throw new RuntimeException(
                    "Invalid reset code"
            );
        }


        // =====================================================
        // FIRST-TIME ACCOUNT SETUP
        // EMPLOYEE / MANAGER / HR
        // =====================================================

        String accountRole =
                user.getRole() == null
                        ? ""
                        : user.getRole()
                        .trim()
                        .toUpperCase(
                                Locale.ROOT
                        );


        boolean supportedFirstTimeRole =
                "EMPLOYEE".equals(
                        accountRole
                )
                        ||
                        "MANAGER".equals(
                                accountRole
                        )
                        ||
                        "HR".equals(
                                accountRole
                        );


        boolean firstTimeAccountSetup =
                supportedFirstTimeRole
                        &&
                        user.getUsername() != null
                        &&
                        user.getUsername()
                                .startsWith(
                                        "pending_"
                                );


        // =====================================================
        // CREATE USERNAME DURING FIRST SETUP
        // =====================================================

        if (firstTimeAccountSetup) {

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


            /*
             * Check username uniqueness.
             */
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


            user.setUsername(
                    chosenUsername
            );


            auditLogService.log(
                    chosenUsername,
                    "ACCOUNT_USERNAME_CREATED",
                    "AUTHENTICATION",
                    user.getId(),
                    accountRole
                            + " selected username during first-time setup",
                    null
            );

        } else if (
                desiredUsername != null
                        &&
                        !desiredUsername.isBlank()
        ) {

            /*
             * Existing username cannot be changed
             * through password reset.
             */
            throw new RuntimeException(
                    "Username can only be selected once during first-time account setup"
            );
        }


        // =====================================================
        // SET NEW PASSWORD
        // =====================================================

        user.setPassword(
                passwordEncoder.encode(
                        newPassword
                )
        );


        userRepository.save(
                user
        );


        // =====================================================
        // MARK OTP AS USED
        // =====================================================

        resetToken.setUsed(
                true
        );


        resetTokenRepository.save(
                resetToken
        );


        // =====================================================
        // REVOKE OLD LOGIN SESSIONS
        // =====================================================

        refreshTokenService
                .revokeAllForUser(
                        user.getUsername()
                );


        // =====================================================
        // AUDIT
        // =====================================================

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


        String normalized =
                value.trim();


        // =====================================================
        // 1. USERNAME
        // =====================================================

        User user =
                userRepository
                        .findByUsername(
                                normalized
                        )
                        .orElse(
                                null
                        );


        if (user != null) {

            return user;
        }


        // =====================================================
        // 2. EMPLOYEE EMAIL
        // =====================================================

        user =
                employeeRepository
                        .findByEmail(
                                normalized
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


        // =====================================================
        // 3. GENERIC RECOVERY EMAIL
        // =====================================================

        List<User> matches =
                userRepository
                        .findAllByRecoveryEmailIgnoreCase(
                                normalized
                        );


        /*
         * Return account only when there is exactly
         * one matching recovery email.
         */
        if (matches.size() == 1) {

            return matches.get(
                    0
            );
        }


        return null;
    }


    // =========================================================
    // RESOLVE RECOVERY EMAIL
    // =========================================================

    private String resolveRecoveryEmail(
            User user
    ) {

        if (user == null) {

            return null;
        }


        // =====================================================
        // USER RECOVERY EMAIL
        // =====================================================

        if (user.getRecoveryEmail() != null
                &&
                !user.getRecoveryEmail()
                        .isBlank()) {

            return user
                    .getRecoveryEmail()
                    .trim();
        }


        // =====================================================
        // FALLBACK TO EMPLOYEE EMAIL
        // =====================================================

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