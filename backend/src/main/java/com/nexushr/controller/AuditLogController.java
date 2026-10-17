package com.nexushr.controller;

import com.nexushr.entity.AuditLog;
import com.nexushr.service.AuditLogService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(
            AuditLogService auditLogService
    ) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ResponseEntity<List<AuditLog>> getAllLogs() {

        return ResponseEntity.ok(
                auditLogService.getAllLogs()
        );
    }

    @GetMapping("/user/{username}")
    public ResponseEntity<List<AuditLog>> getLogsByUsername(
            @PathVariable String username
    ) {

        return ResponseEntity.ok(
                auditLogService.getLogsByUsername(username)
        );
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    public ResponseEntity<List<AuditLog>> getLogsByEntity(
            @PathVariable String entityType,
            @PathVariable Long entityId
    ) {

        return ResponseEntity.ok(
                auditLogService.getLogsByEntity(
                        entityType,
                        entityId
                )
        );
    }

    @GetMapping("/action/{action}")
    public ResponseEntity<List<AuditLog>> getLogsByAction(
            @PathVariable String action
    ) {

        return ResponseEntity.ok(
                auditLogService.getLogsByAction(action)
        );
    }
}