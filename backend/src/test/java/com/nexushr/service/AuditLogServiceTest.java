package com.nexushr.service;

import com.nexushr.entity.AuditLog;
import com.nexushr.repository.AuditLogRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {

        SecurityContextHolder.clearContext();

        SecurityContextHolder
                .getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                "admin",
                                "password",
                                List.of()
                        )
                );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void logShouldCreateAuditRecordUsingAuthenticatedUser() {

        when(auditLogRepository.save(any(AuditLog.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        AuditLog result =
                auditLogService.log(
                        "EMPLOYEE_CREATED",
                        "EMPLOYEE",
                        10L,
                        "Employee created successfully"
                );

        assertNotNull(result);

        assertEquals(
                "admin",
                result.getUsername()
        );

        assertEquals(
                "EMPLOYEE_CREATED",
                result.getAction()
        );

        assertEquals(
                "EMPLOYEE",
                result.getEntityType()
        );

        assertEquals(
                10L,
                result.getEntityId()
        );

        assertEquals(
                "Employee created successfully",
                result.getDescription()
        );

        assertNull(result.getIpAddress());

        assertNotNull(result.getCreatedAt());

        verify(auditLogRepository)
                .save(any(AuditLog.class));
    }

    @Test
    void logShouldStoreIpAddress() {

        when(auditLogRepository.save(any(AuditLog.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        AuditLog result =
                auditLogService.log(
                        "EMPLOYEE_UPDATED",
                        "EMPLOYEE",
                        20L,
                        "Employee updated",
                        "127.0.0.1"
                );

        assertNotNull(result);

        assertEquals(
                "admin",
                result.getUsername()
        );

        assertEquals(
                "EMPLOYEE_UPDATED",
                result.getAction()
        );

        assertEquals(
                "127.0.0.1",
                result.getIpAddress()
        );

        verify(auditLogRepository)
                .save(any(AuditLog.class));
    }

    @Test
    void explicitUsernameShouldBeStoredWhenProvided() {

        when(auditLogRepository.save(any(AuditLog.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        AuditLog result =
                auditLogService.log(
                        "system-user",
                        "SYSTEM_OPERATION",
                        "SYSTEM",
                        null,
                        "System operation completed",
                        null
                );

        assertNotNull(result);

        assertEquals(
                "system-user",
                result.getUsername()
        );

        assertEquals(
                "SYSTEM_OPERATION",
                result.getAction()
        );

        assertEquals(
                "SYSTEM",
                result.getEntityType()
        );

        assertEquals(
                "System operation completed",
                result.getDescription()
        );

        verify(auditLogRepository)
                .save(any(AuditLog.class));
    }

    @Test
    void unauthenticatedLogShouldUseSystemUsername() {

        SecurityContextHolder.clearContext();

        when(auditLogRepository.save(any(AuditLog.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        AuditLog result =
                auditLogService.log(
                        "SYSTEM_OPERATION",
                        "SYSTEM",
                        null,
                        "System operation"
                );

        assertNotNull(result);

        assertEquals(
                "SYSTEM",
                result.getUsername()
        );

        verify(auditLogRepository)
                .save(any(AuditLog.class));
    }

    @Test
    void anonymousUserShouldUseSystemUsername() {

        SecurityContextHolder.clearContext();

        SecurityContextHolder
                .getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                "anonymousUser",
                                null,
                                List.of()
                        )
                );

        when(auditLogRepository.save(any(AuditLog.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        AuditLog result =
                auditLogService.log(
                        "SYSTEM_OPERATION",
                        "SYSTEM",
                        null,
                        "Anonymous operation"
                );

        assertNotNull(result);

        assertEquals(
                "SYSTEM",
                result.getUsername()
        );

        verify(auditLogRepository)
                .save(any(AuditLog.class));
    }

    @Test
    void logShouldRejectBlankAction() {

        assertThrows(
                IllegalArgumentException.class,
                () -> auditLogService.log(
                        "",
                        "EMPLOYEE",
                        1L,
                        "Test"
                )
        );

        verify(
                auditLogRepository,
                never()
        ).save(any(AuditLog.class));
    }

    @Test
    void logShouldRejectBlankEntityType() {

        assertThrows(
                IllegalArgumentException.class,
                () -> auditLogService.log(
                        "EMPLOYEE_CREATED",
                        "",
                        1L,
                        "Test"
                )
        );

        verify(
                auditLogRepository,
                never()
        ).save(any(AuditLog.class));
    }

    @Test
    void logShouldRejectBlankDescription() {

        assertThrows(
                IllegalArgumentException.class,
                () -> auditLogService.log(
                        "EMPLOYEE_CREATED",
                        "EMPLOYEE",
                        1L,
                        ""
                )
        );

        verify(
                auditLogRepository,
                never()
        ).save(any(AuditLog.class));
    }

    @Test
    void logShouldRejectNullAction() {

        assertThrows(
                IllegalArgumentException.class,
                () -> auditLogService.log(
                        null,
                        "EMPLOYEE",
                        1L,
                        "Test"
                )
        );

        verify(
                auditLogRepository,
                never()
        ).save(any(AuditLog.class));
    }

    @Test
    void logShouldRejectNullEntityType() {

        assertThrows(
                IllegalArgumentException.class,
                () -> auditLogService.log(
                        "EMPLOYEE_CREATED",
                        null,
                        1L,
                        "Test"
                )
        );

        verify(
                auditLogRepository,
                never()
        ).save(any(AuditLog.class));
    }

    @Test
    void logShouldRejectNullDescription() {

        assertThrows(
                IllegalArgumentException.class,
                () -> auditLogService.log(
                        "EMPLOYEE_CREATED",
                        "EMPLOYEE",
                        1L,
                        null
                )
        );

        verify(
                auditLogRepository,
                never()
        ).save(any(AuditLog.class));
    }

    @Test
    void getAllLogsShouldReturnNewestFirstResults() {

        AuditLog first =
                new AuditLog(
                        "admin",
                        "EMPLOYEE_CREATED",
                        "EMPLOYEE",
                        1L,
                        "Created",
                        null
                );

        AuditLog second =
                new AuditLog(
                        "hr",
                        "EMPLOYEE_UPDATED",
                        "EMPLOYEE",
                        1L,
                        "Updated",
                        null
                );

        when(auditLogRepository
                .findAllByOrderByCreatedAtDesc())
                .thenReturn(
                        List.of(first, second)
                );

        List<AuditLog> result =
                auditLogService.getAllLogs();

        assertNotNull(result);

        assertEquals(
                2,
                result.size()
        );

        assertSame(
                first,
                result.get(0)
        );

        assertSame(
                second,
                result.get(1)
        );

        verify(auditLogRepository)
                .findAllByOrderByCreatedAtDesc();
    }

    @Test
    void getLogsByUsernameShouldReturnMatchingLogs() {

        List<AuditLog> logs =
                List.of(
                        new AuditLog(
                                "admin",
                                "EMPLOYEE_CREATED",
                                "EMPLOYEE",
                                1L,
                                "Created",
                                null
                        )
                );

        when(auditLogRepository
                .findByUsernameOrderByCreatedAtDesc(
                        "admin"
                ))
                .thenReturn(logs);

        List<AuditLog> result =
                auditLogService.getLogsByUsername(
                        "admin"
                );

        assertNotNull(result);

        assertEquals(
                1,
                result.size()
        );

        assertEquals(
                "admin",
                result.get(0).getUsername()
        );

        verify(auditLogRepository)
                .findByUsernameOrderByCreatedAtDesc(
                        "admin"
                );
    }

    @Test
    void getLogsByUsernameShouldRejectBlankUsername() {

        assertThrows(
                IllegalArgumentException.class,
                () -> auditLogService.getLogsByUsername("")
        );

        verify(
                auditLogRepository,
                never()
        ).findByUsernameOrderByCreatedAtDesc(
                anyString()
        );
    }

    @Test
    void getLogsByUsernameShouldRejectNullUsername() {

        assertThrows(
                IllegalArgumentException.class,
                () -> auditLogService.getLogsByUsername(null)
        );

        verify(
                auditLogRepository,
                never()
        ).findByUsernameOrderByCreatedAtDesc(
                anyString()
        );
    }

    @Test
    void getLogsByEntityShouldReturnMatchingLogs() {

        List<AuditLog> logs =
                List.of(
                        new AuditLog(
                                "admin",
                                "EMPLOYEE_CREATED",
                                "EMPLOYEE",
                                5L,
                                "Created employee",
                                null
                        )
                );

        when(auditLogRepository
                .findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
                        "EMPLOYEE",
                        5L
                ))
                .thenReturn(logs);

        List<AuditLog> result =
                auditLogService.getLogsByEntity(
                        "EMPLOYEE",
                        5L
                );

        assertNotNull(result);

        assertEquals(
                1,
                result.size()
        );

        assertEquals(
                "EMPLOYEE",
                result.get(0).getEntityType()
        );

        assertEquals(
                5L,
                result.get(0).getEntityId()
        );

        verify(auditLogRepository)
                .findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
                        "EMPLOYEE",
                        5L
                );
    }

    @Test
    void getLogsByEntityShouldRejectMissingEntityType() {

        assertThrows(
                IllegalArgumentException.class,
                () -> auditLogService.getLogsByEntity(
                        "",
                        1L
                )
        );

        verify(
                auditLogRepository,
                never()
        ).findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
                anyString(),
                anyLong()
        );
    }

    @Test
    void getLogsByEntityShouldRejectMissingEntityId() {

        assertThrows(
                IllegalArgumentException.class,
                () -> auditLogService.getLogsByEntity(
                        "EMPLOYEE",
                        null
                )
        );

        verify(
                auditLogRepository,
                never()
        ).findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
                anyString(),
                any()
        );
    }

    @Test
    void getLogsByActionShouldReturnMatchingLogs() {

        List<AuditLog> logs =
                List.of(
                        new AuditLog(
                                "admin",
                                "EMPLOYEE_CREATED",
                                "EMPLOYEE",
                                1L,
                                "Created",
                                null
                        )
                );

        when(auditLogRepository
                .findByActionOrderByCreatedAtDesc(
                        "EMPLOYEE_CREATED"
                ))
                .thenReturn(logs);

        List<AuditLog> result =
                auditLogService.getLogsByAction(
                        "EMPLOYEE_CREATED"
                );

        assertNotNull(result);

        assertEquals(
                1,
                result.size()
        );

        assertEquals(
                "EMPLOYEE_CREATED",
                result.get(0).getAction()
        );

        verify(auditLogRepository)
                .findByActionOrderByCreatedAtDesc(
                        "EMPLOYEE_CREATED"
                );
    }

    @Test
    void getLogsByActionShouldRejectBlankAction() {

        assertThrows(
                IllegalArgumentException.class,
                () -> auditLogService.getLogsByAction("")
        );

        verify(
                auditLogRepository,
                never()
        ).findByActionOrderByCreatedAtDesc(
                anyString()
        );
    }

    @Test
    void getLogsByActionShouldRejectNullAction() {

        assertThrows(
                IllegalArgumentException.class,
                () -> auditLogService.getLogsByAction(null)
        );

        verify(
                auditLogRepository,
                never()
        ).findByActionOrderByCreatedAtDesc(
                anyString()
        );
    }
}