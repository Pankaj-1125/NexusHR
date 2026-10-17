package com.nexushr.service;

import com.nexushr.dto.NotificationDTO;
import com.nexushr.entity.Notification;
import com.nexushr.repository.EmployeeRepository;
import com.nexushr.repository.NotificationRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmployeeRepository employeeRepository;
    private final AuditLogService auditLogService;

    @Autowired(required = false)
    private RealtimeNotificationPublisher realtimeNotificationPublisher;

    public NotificationService(
            NotificationRepository notificationRepository,
            EmployeeRepository employeeRepository,
            AuditLogService auditLogService
    ) {
        this.notificationRepository =
                notificationRepository;

        this.employeeRepository =
                employeeRepository;

        this.auditLogService =
                auditLogService;
    }

    @Transactional
    public NotificationDTO createNotification(
            Long employeeId,
            String title,
            String message,
            String type
    ) {
        return createNotification(
                employeeId,
                title,
                message,
                type,
                "SYSTEM",
                "SYSTEM"
        );
    }

    @Transactional
    public NotificationDTO createNotification(
            Long employeeId,
            String title,
            String message,
            String type,
            String senderUsername,
            String senderRole
    ) {

        if (employeeId == null) {
            throw new RuntimeException(
                    "Employee ID is required"
            );
        }

        if (!employeeRepository.existsById(employeeId)) {
            throw new RuntimeException(
                    "Employee not found with ID: "
                            + employeeId
            );
        }

        if (title == null || title.isBlank()) {
            throw new RuntimeException(
                    "Notification title is required"
            );
        }

        if (message == null || message.isBlank()) {
            throw new RuntimeException(
                    "Notification message is required"
            );
        }

        Notification notification =
                new Notification(
                        employeeId,
                        title.trim(),
                        message.trim(),
                        type == null || type.isBlank()
                                ? "GENERAL"
                                : type.trim().toUpperCase(),
                        senderUsername == null || senderUsername.isBlank()
                                ? "SYSTEM"
                                : senderUsername.trim(),
                        senderRole == null || senderRole.isBlank()
                                ? "SYSTEM"
                                : senderRole.trim().toUpperCase()
                );

        Notification saved =
                notificationRepository.save(notification);

        auditLogService.log(
                "NOTIFICATION_CREATED",
                "NOTIFICATION",
                saved.getId(),
                "Notification created for employee ID: "
                        + employeeId
        );

        NotificationDTO dto =
                convertToDTO(saved);

        if (realtimeNotificationPublisher != null) {
            realtimeNotificationPublisher.publish(dto);
        }

        return dto;
    }

    public List<NotificationDTO> getEmployeeNotifications(
            Long employeeId
    ) {

        verifyEmployee(employeeId);

        return notificationRepository
                .findByEmployeeIdOrderByCreatedAtDesc(
                        employeeId
                )
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<NotificationDTO> getUnreadNotifications(
            Long employeeId
    ) {

        verifyEmployee(employeeId);

        return notificationRepository
                .findByEmployeeIdAndReadFalseOrderByCreatedAtDesc(
                        employeeId
                )
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public long countUnreadNotifications(
            Long employeeId
    ) {

        verifyEmployee(employeeId);

        return notificationRepository
                .countByEmployeeIdAndReadFalse(
                        employeeId
                );
    }

    @Transactional
    public NotificationDTO markAsRead(
            Long notificationId
    ) {

        Notification notification =
                notificationRepository
                        .findById(notificationId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Notification not found with ID: "
                                                + notificationId
                                )
                        );

        notification.setRead(true);

        Notification saved =
                notificationRepository.save(notification);

        auditLogService.log(
                "NOTIFICATION_READ",
                "NOTIFICATION",
                saved.getId(),
                "Notification marked as read"
        );

        NotificationDTO dto =
                convertToDTO(saved);

        if (realtimeNotificationPublisher != null) {
            realtimeNotificationPublisher.publish(dto);
        }

        return dto;
    }

    @Transactional
    public int markAllAsRead(
            Long employeeId
    ) {

        verifyEmployee(employeeId);

        List<Notification> notifications =
                notificationRepository
                        .findByEmployeeIdAndReadFalseOrderByCreatedAtDesc(
                                employeeId
                        );

        notifications.forEach(
                notification ->
                        notification.setRead(true)
        );

        if (!notifications.isEmpty()) {
            notificationRepository.saveAll(
                    notifications
            );

            auditLogService.log(
                    "NOTIFICATIONS_READ_ALL",
                    "EMPLOYEE",
                    employeeId,
                    "Marked "
                            + notifications.size()
                            + " notification(s) as read"
            );
        }

        return notifications.size();
    }

    public List<NotificationDTO> getAllNotifications() {

        return notificationRepository
                .findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public java.util.Map<String, Object> getEmployeeSummary(
            Long employeeId
    ) {

        verifyEmployee(employeeId);

        List<Notification> rows =
                notificationRepository
                        .findByEmployeeIdOrderByCreatedAtDesc(
                                employeeId
                        );

        long unread =
                rows.stream()
                        .filter(notification -> !notification.isRead())
                        .count();

        java.util.Map<String, Long> byType =
                rows.stream()
                        .collect(
                                java.util.stream.Collectors.groupingBy(
                                        notification ->
                                                notification.getType() == null
                                                        ? "GENERAL"
                                                        : notification.getType(),
                                        java.util.LinkedHashMap::new,
                                        java.util.stream.Collectors.counting()
                                )
                        );

        java.util.Map<String, Object> summary =
                new java.util.LinkedHashMap<>();

        summary.put("employeeId", employeeId);
        summary.put("total", rows.size());
        summary.put("unread", unread);
        summary.put("read", rows.size() - unread);
        summary.put("byType", byType);

        return summary;
    }

    @Transactional
    public void deleteNotification(Long notificationId) {

        Notification notification =
                notificationRepository
                        .findById(notificationId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Notification not found with ID: "
                                                + notificationId
                                )
                        );

        notificationRepository.delete(notification);

        auditLogService.log(
                "NOTIFICATION_DELETED",
                "NOTIFICATION",
                notificationId,
                "Notification deleted"
        );
    }

    private void verifyEmployee(
            Long employeeId
    ) {

        if (employeeId == null) {
            throw new RuntimeException(
                    "Employee ID is required"
            );
        }

        if (!employeeRepository.existsById(employeeId)) {
            throw new RuntimeException(
                    "Employee not found with ID: "
                            + employeeId
            );
        }
    }

    private NotificationDTO convertToDTO(
            Notification notification
    ) {

        return new NotificationDTO(
                notification.getId(),
                notification.getEmployeeId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getType(),
                notification.getSenderUsername(),
                notification.getSenderRole(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}