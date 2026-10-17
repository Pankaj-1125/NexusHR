package com.nexushr.service;

import com.nexushr.dto.NotificationDTO;
import com.nexushr.entity.Notification;
import com.nexushr.repository.EmployeeRepository;
import com.nexushr.repository.NotificationRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private NotificationService notificationService;

    private Notification notification;

    @BeforeEach
    void setUp() {
        notification =
                new Notification(
                        1L,
                        "Test notification",
                        "Test message",
                        "GENERAL"
                );

        notification.setId(10L);
    }

    @Test
    void shouldCreateNotificationAndAudit() {

        when(employeeRepository.existsById(1L))
                .thenReturn(true);

        when(notificationRepository.save(any(Notification.class)))
                .thenReturn(notification);

        NotificationDTO result =
                notificationService.createNotification(
                        1L,
                        "Test notification",
                        "Test message",
                        "general"
                );

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals(1L, result.getEmployeeId());
        assertEquals("Test notification", result.getTitle());
        assertEquals("GENERAL", result.getType());

        verify(auditLogService)
                .log(
                        eq("NOTIFICATION_CREATED"),
                        eq("NOTIFICATION"),
                        eq(10L),
                        contains("employee ID: 1")
                );
    }

    @Test
    void shouldRejectNotificationForMissingEmployee() {

        when(employeeRepository.existsById(99L))
                .thenReturn(false);

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () ->
                                notificationService
                                        .createNotification(
                                                99L,
                                                "Title",
                                                "Message",
                                                "GENERAL"
                                        )
                );

        assertEquals(
                "Employee not found with ID: 99",
                exception.getMessage()
        );

        verify(notificationRepository, never())
                .save(any());

        verifyNoInteractions(auditLogService);
    }

    @Test
    void shouldRejectBlankTitle() {

        when(employeeRepository.existsById(1L))
                .thenReturn(true);

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () ->
                                notificationService
                                        .createNotification(
                                                1L,
                                                " ",
                                                "Message",
                                                "GENERAL"
                                        )
                );

        assertEquals(
                "Notification title is required",
                exception.getMessage()
        );

        verify(notificationRepository, never())
                .save(any());
    }

    @Test
    void shouldRejectBlankMessage() {

        when(employeeRepository.existsById(1L))
                .thenReturn(true);

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () ->
                                notificationService
                                        .createNotification(
                                                1L,
                                                "Title",
                                                " ",
                                                "GENERAL"
                                        )
                );

        assertEquals(
                "Notification message is required",
                exception.getMessage()
        );

        verify(notificationRepository, never())
                .save(any());
    }

    @Test
    void shouldGetEmployeeNotifications() {

        when(employeeRepository.existsById(1L))
                .thenReturn(true);

        when(
                notificationRepository
                        .findByEmployeeIdOrderByCreatedAtDesc(1L)
        ).thenReturn(List.of(notification));

        List<NotificationDTO> result =
                notificationService
                        .getEmployeeNotifications(1L);

        assertEquals(1, result.size());
        assertEquals(
                "Test notification",
                result.get(0).getTitle()
        );
    }

    @Test
    void shouldGetUnreadNotifications() {

        when(employeeRepository.existsById(1L))
                .thenReturn(true);

        when(
                notificationRepository
                        .findByEmployeeIdAndReadFalseOrderByCreatedAtDesc(
                                1L
                        )
        ).thenReturn(List.of(notification));

        List<NotificationDTO> result =
                notificationService
                        .getUnreadNotifications(1L);

        assertEquals(1, result.size());
    }

    @Test
    void shouldCountUnreadNotifications() {

        when(employeeRepository.existsById(1L))
                .thenReturn(true);

        when(
                notificationRepository
                        .countByEmployeeIdAndReadFalse(1L)
        ).thenReturn(4L);

        long result =
                notificationService
                        .countUnreadNotifications(1L);

        assertEquals(4L, result);
    }

    @Test
    void shouldMarkNotificationAsReadAndAudit() {

        notification.setRead(false);

        when(
                notificationRepository.findById(10L)
        ).thenReturn(Optional.of(notification));

        when(
                notificationRepository.save(notification)
        ).thenReturn(notification);

        NotificationDTO result =
                notificationService.markAsRead(10L);

        assertTrue(result.isRead());

        verify(auditLogService)
                .log(
                        eq("NOTIFICATION_READ"),
                        eq("NOTIFICATION"),
                        eq(10L),
                        eq("Notification marked as read")
                );
    }

    @Test
    void shouldRejectUnknownNotification() {

        when(
                notificationRepository.findById(999L)
        ).thenReturn(Optional.empty());

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () ->
                                notificationService
                                        .markAsRead(999L)
                );

        assertEquals(
                "Notification not found with ID: 999",
                exception.getMessage()
        );

        verifyNoInteractions(auditLogService);
    }

    @Test
    void shouldMarkAllAsReadAndAudit() {

        notification.setRead(false);

        Notification second =
                new Notification(
                        1L,
                        "Second",
                        "Second message",
                        "LEAVE"
                );

        second.setId(11L);

        when(employeeRepository.existsById(1L))
                .thenReturn(true);

        when(
                notificationRepository
                        .findByEmployeeIdAndReadFalseOrderByCreatedAtDesc(
                                1L
                        )
        ).thenReturn(List.of(notification, second));

        int result =
                notificationService.markAllAsRead(1L);

        assertEquals(2, result);
        assertTrue(notification.isRead());
        assertTrue(second.isRead());

        verify(notificationRepository)
                .saveAll(anyList());

        verify(auditLogService)
                .log(
                        eq("NOTIFICATIONS_READ_ALL"),
                        eq("EMPLOYEE"),
                        eq(1L),
                        contains("2 notification(s)")
                );
    }

    @Test
    void shouldNotAuditWhenThereAreNoUnreadNotifications() {

        when(employeeRepository.existsById(1L))
                .thenReturn(true);

        when(
                notificationRepository
                        .findByEmployeeIdAndReadFalseOrderByCreatedAtDesc(
                                1L
                        )
        ).thenReturn(List.of());

        int result =
                notificationService.markAllAsRead(1L);

        assertEquals(0, result);

        verify(notificationRepository, never())
                .saveAll(anyList());

        verifyNoInteractions(auditLogService);
    }
}