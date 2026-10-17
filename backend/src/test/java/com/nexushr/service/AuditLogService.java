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

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(
            AuditLogRepository auditLogRepository
    ) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public AuditLog log(
            String action,
            String entityType,
            Long entityId,
            String description
    ) {
        return log(
                getCurrentUsername(),
                action,
                entityType,
                entityId,
                description,
                null
        );
    }

    @Transactional
    public AuditLog log(
            String action,
            String entityType,
            Long entityId,
            String description,
            String ipAddress
    ) {
        return log(
                getCurrentUsername(),
                action,
                entityType,
                entityId,
                description,
                ipAddress
        );
    }

    @Transactional
    public AuditLog log(
            String username,
            String action,
            String entityType,
            Long entityId,
            String description,
            String ipAddress
    ) {

        String safeUsername =
                username == null ||
                        username.isBlank()
                        ? "SYSTEM"
                        : username;

        String safeAction =
                requireValue(action, "Audit action");

        String safeEntityType =
                requireValue(entityType, "Audit entity type");

        String safeDescription =
                requireValue(description, "Audit description");

        AuditLog auditLog =
                new AuditLog(
                        safeUsername,
                        safeAction,
                        safeEntityType,
                        entityId,
                        safeDescription,
                        ipAddress
                );

        return auditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getAllLogs() {
        return auditLogRepository
                .findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getLogsByUsername(
            String username
    ) {

        if (username == null ||
                username.isBlank()) {

            throw new IllegalArgumentException(
                    "Username is required."
            );
        }

        return auditLogRepository
                .findByUsernameOrderByCreatedAtDesc(
                        username
                );
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getLogsByEntity(
            String entityType,
            Long entityId
    ) {

        if (entityType == null ||
                entityType.isBlank()) {

            throw new IllegalArgumentException(
                    "Entity type is required."
            );
        }

        if (entityId == null) {

            throw new IllegalArgumentException(
                    "Entity ID is required."
            );
        }

        return auditLogRepository
                .findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
                        entityType,
                        entityId
                );
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getLogsByAction(
            String action
    ) {

        if (action == null ||
                action.isBlank()) {

            throw new IllegalArgumentException(
                    "Action is required."
            );
        }

        return auditLogRepository
                .findByActionOrderByCreatedAtDesc(
                        action
                );
    }

    private String getCurrentUsername() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated()) {

            return "SYSTEM";
        }

        String username =
                authentication.getName();

        if (username == null ||
                username.isBlank() ||
                "anonymousUser".equalsIgnoreCase(username)) {

            return "SYSTEM";
        }

        return username;
    }

    private String requireValue(
            String value,
            String fieldName
    ) {

        if (value == null ||
                value.isBlank()) {

            throw new IllegalArgumentException(
                    fieldName + " is required."
            );
        }

        return value.trim();
    }
}