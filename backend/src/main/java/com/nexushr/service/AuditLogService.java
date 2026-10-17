package com.nexushr.service;

import com.nexushr.entity.AuditLog;
import com.nexushr.repository.AuditLogRepository;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditLogService {

    private static final String SYSTEM_USER = "SYSTEM";

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(
            AuditLogRepository auditLogRepository) {

        this.auditLogRepository = auditLogRepository;
    }

    // =========================================================
    // LOG USING AUTHENTICATED USER
    // =========================================================

    @Transactional
    public AuditLog log(
            String action,
            String entityType,
            Long entityId,
            String description) {

        String username =
                resolveCurrentUsername();

        return saveAuditLog(
                username,
                action,
                entityType,
                entityId,
                description,
                null
        );
    }

    // =========================================================
    // LOG WITH IP ADDRESS
    // =========================================================

    @Transactional
    public AuditLog log(
            String action,
            String entityType,
            Long entityId,
            String description,
            String ipAddress) {

        String username =
                resolveCurrentUsername();

        return saveAuditLog(
                username,
                action,
                entityType,
                entityId,
                description,
                ipAddress
        );
    }

    // =========================================================
    // LOG WITH EXPLICIT USERNAME
    // =========================================================

    @Transactional
    public AuditLog log(
            String username,
            String action,
            String entityType,
            Long entityId,
            String description,
            String ipAddress) {

        String resolvedUsername =
                username == null ||
                        username.isBlank()
                        ? resolveCurrentUsername()
                        : username.trim();

        return saveAuditLog(
                resolvedUsername,
                action,
                entityType,
                entityId,
                description,
                ipAddress
        );
    }

    // =========================================================
    // SAVE AUDIT LOG
    // =========================================================

    private AuditLog saveAuditLog(
            String username,
            String action,
            String entityType,
            Long entityId,
            String description,
            String ipAddress) {

        validateRequiredField(
                action,
                "Action is required."
        );

        validateRequiredField(
                entityType,
                "Entity type is required."
        );

        validateRequiredField(
                description,
                "Description is required."
        );

        String resolvedUsername =
                username == null ||
                        username.isBlank()
                        ? SYSTEM_USER
                        : username.trim();

        AuditLog auditLog =
                new AuditLog(
                        resolvedUsername,
                        action.trim(),
                        entityType.trim(),
                        entityId,
                        description.trim(),
                        ipAddress
                );

        return auditLogRepository.save(auditLog);
    }

    // =========================================================
    // CURRENT USER
    // =========================================================

    private String resolveCurrentUsername() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated()) {

            return SYSTEM_USER;
        }

        String username =
                authentication.getName();

        if (username == null ||
                username.isBlank() ||
                "anonymousUser".equalsIgnoreCase(username)) {

            return SYSTEM_USER;
        }

        return username;
    }

    // =========================================================
    // GET ALL LOGS
    // =========================================================

    @Transactional(readOnly = true)
    public List<AuditLog> getAllLogs() {

        return auditLogRepository
                .findAllByOrderByCreatedAtDesc();
    }

    // =========================================================
    // GET LOGS BY USERNAME
    // =========================================================

    @Transactional(readOnly = true)
    public List<AuditLog> getLogsByUsername(
            String username) {

        validateRequiredField(
                username,
                "Username is required."
        );

        return auditLogRepository
                .findByUsernameOrderByCreatedAtDesc(
                        username.trim()
                );
    }

    // =========================================================
    // GET LOGS BY ENTITY
    // =========================================================

    @Transactional(readOnly = true)
    public List<AuditLog> getLogsByEntity(
            String entityType,
            Long entityId) {

        validateRequiredField(
                entityType,
                "Entity type is required."
        );

        if (entityId == null) {
            throw new IllegalArgumentException(
                    "Entity ID is required."
            );
        }

        return auditLogRepository
                .findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
                        entityType.trim(),
                        entityId
                );
    }

    // =========================================================
    // GET LOGS BY ACTION
    // =========================================================

    @Transactional(readOnly = true)
    public List<AuditLog> getLogsByAction(
            String action) {

        validateRequiredField(
                action,
                "Action is required."
        );

        return auditLogRepository
                .findByActionOrderByCreatedAtDesc(
                        action.trim()
                );
    }

    // =========================================================
    // VALIDATION
    // =========================================================

    private void validateRequiredField(
            String value,
            String message) {

        if (value == null ||
                value.isBlank()) {

            throw new IllegalArgumentException(
                    message
            );
        }
    }
}