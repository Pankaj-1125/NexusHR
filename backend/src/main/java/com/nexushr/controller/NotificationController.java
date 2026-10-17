package com.nexushr.controller;

import com.nexushr.dto.NotificationDTO;
import com.nexushr.entity.Employee;
import com.nexushr.repository.EmployeeRepository;
import com.nexushr.service.EmailNotificationService;
import com.nexushr.service.NotificationService;
import com.nexushr.service.SmsNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final EmployeeRepository employeeRepository;
    private final EmailNotificationService emailNotificationService;
    private final SmsNotificationService smsNotificationService;

    public NotificationController(
            NotificationService notificationService,
            EmployeeRepository employeeRepository,
            EmailNotificationService emailNotificationService,
            SmsNotificationService smsNotificationService
    ) {
        this.notificationService = notificationService;
        this.employeeRepository = employeeRepository;
        this.emailNotificationService = emailNotificationService;
        this.smsNotificationService = smsNotificationService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> createNotification(
            @RequestBody CreateNotificationRequest request,
            Authentication authentication
    ) {
        validateText(request.title(), request.message());
        Employee employee = findEmployee(request.employeeId());
        String sender = senderUsername(authentication);
        String role = senderRole(authentication);

        NotificationDTO created = notificationService.createNotification(
                request.employeeId(),
                request.title(),
                request.message(),
                request.type(),
                sender,
                role
        );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("notification", created);
        response.put("senderUsername", sender);
        response.put("senderRole", role);

        if (Boolean.TRUE.equals(request.sendEmail())) {
            response.put(
                    "email",
                    emailNotificationService.send(
                            employee,
                            request.title(),
                            buildEmailBody(sender, role, request.message())
                    )
            );
        }

        if (Boolean.TRUE.equals(request.sendSms())) {
            response.put(
                    "sms",
                    smsNotificationService.sendSms(
                            employee,
                            request.title(),
                            request.message()
                    )
            );
        }

        response.put("message", "Communication sent successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/broadcast")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> broadcastNotification(
            @RequestBody BroadcastNotificationRequest request,
            Authentication authentication
    ) {
        validateText(request.title(), request.message());

        String sender = senderUsername(authentication);
        String role = senderRole(authentication);
        String targetDepartment = request.department() == null
                ? ""
                : request.department().trim();

        List<Employee> employees = employeeRepository
                .findAll()
                .stream()
                .filter(this::isActiveEmployee)
                .filter(employee ->
                        targetDepartment.isBlank()
                                || "ALL".equalsIgnoreCase(targetDepartment)
                                || (employee.getDepartment() != null
                                && employee.getDepartment().equalsIgnoreCase(targetDepartment))
                )
                .toList();

        int createdCount = 0;
        int emailSent = 0;
        int emailSkipped = 0;
        int emailFailed = 0;
        int smsSent = 0;
        int smsSkipped = 0;
        int smsFailed = 0;
        String smsMode = "NONE";

        for (Employee employee : employees) {
            notificationService.createNotification(
                    employee.getId(),
                    request.title(),
                    request.message(),
                    request.type() == null || request.type().isBlank()
                            ? "ANNOUNCEMENT"
                            : request.type(),
                    sender,
                    role
            );
            createdCount++;

            if (Boolean.TRUE.equals(request.sendEmail())) {
                EmailNotificationService.DeliveryResult result =
                        emailNotificationService.send(
                                employee,
                                request.title(),
                                buildEmailBody(sender, role, request.message())
                        );
                switch (result.status()) {
                    case "SENT" -> emailSent++;
                    case "FAILED" -> emailFailed++;
                    default -> emailSkipped++;
                }
            }

            if (Boolean.TRUE.equals(request.sendSms())) {
                SmsNotificationService.DeliveryResult result =
                        smsNotificationService.sendSms(
                                employee,
                                request.title(),
                                request.message()
                        );
                smsMode = result.mode();
                switch (result.status()) {
                    case "SENT" -> smsSent++;
                    case "FAILED" -> smsFailed++;
                    default -> smsSkipped++;
                }
            }
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("createdCount", createdCount);
        response.put("department", targetDepartment.isBlank() ? "ALL" : targetDepartment);
        response.put("senderUsername", sender);
        response.put("senderRole", role);
        response.put("message", "Communication sent to " + createdCount + " active employee(s)");

        if (Boolean.TRUE.equals(request.sendEmail())) {
            response.put("email", Map.of(
                    "sent", emailSent,
                    "skipped", emailSkipped,
                    "failed", emailFailed
            ));
        }

        if (Boolean.TRUE.equals(request.sendSms())) {
            response.put("sms", Map.of(
                    "sent", smsSent,
                    "skipped", smsSkipped,
                    "failed", smsFailed,
                    "mode", smsMode
            ));
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<List<NotificationDTO>> getAllNotifications() {
        return ResponseEntity.ok(notificationService.getAllNotifications());
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<NotificationDTO>> getEmployeeNotifications(
            @PathVariable Long employeeId
    ) {
        return ResponseEntity.ok(notificationService.getEmployeeNotifications(employeeId));
    }

    @GetMapping("/employee/{employeeId}/unread")
    public ResponseEntity<List<NotificationDTO>> getUnreadNotifications(
            @PathVariable Long employeeId
    ) {
        return ResponseEntity.ok(notificationService.getUnreadNotifications(employeeId));
    }

    @GetMapping("/employee/{employeeId}/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @PathVariable Long employeeId
    ) {
        return ResponseEntity.ok(Map.of(
                "unreadCount",
                notificationService.countUnreadNotifications(employeeId)
        ));
    }

    @GetMapping("/employee/{employeeId}/summary")
    public ResponseEntity<Map<String, Object>> getSummary(
            @PathVariable Long employeeId
    ) {
        return ResponseEntity.ok(notificationService.getEmployeeSummary(employeeId));
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<NotificationDTO> markAsRead(
            @PathVariable Long notificationId
    ) {
        return ResponseEntity.ok(notificationService.markAsRead(notificationId));
    }

    @PutMapping("/employee/{employeeId}/read-all")
    public ResponseEntity<Map<String, Integer>> markAllAsRead(
            @PathVariable Long employeeId
    ) {
        return ResponseEntity.ok(Map.of(
                "updatedCount",
                notificationService.markAllAsRead(employeeId)
        ));
    }

    @DeleteMapping("/{notificationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<Map<String, String>> deleteNotification(
            @PathVariable Long notificationId
    ) {
        notificationService.deleteNotification(notificationId);
        return ResponseEntity.ok(Map.of("message", "Notification deleted successfully"));
    }

    private Employee findEmployee(Long employeeId) {
        if (employeeId == null) {
            throw new RuntimeException("Employee ID is required");
        }
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found with ID: " + employeeId));
    }

    private void validateText(String title, String message) {
        if (title == null || title.isBlank()) {
            throw new RuntimeException("Subject is required");
        }
        if (message == null || message.isBlank()) {
            throw new RuntimeException("Message is required");
        }
    }

    private String senderUsername(Authentication authentication) {
        return authentication == null || authentication.getName() == null
                ? "SYSTEM"
                : authentication.getName();
    }

    private String senderRole(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return "SYSTEM";
        }
        return authentication.getAuthorities()
                .stream()
                .map(authority -> authority.getAuthority())
                .filter(value -> value != null && value.startsWith("ROLE_"))
                .map(value -> value.substring(5))
                .findFirst()
                .orElse("SYSTEM");
    }

    private String buildEmailBody(String sender, String role, String message) {
        return "NexusHR Communication\n\n"
                + message.trim()
                + "\n\n----------------------------------------\n"
                + "Sent by: " + sender + " (" + role + ")\n"
                + "NexusHR Workforce Intelligence";
    }

    private boolean isActiveEmployee(Employee employee) {
        return employee.getEmploymentStatus() == null
                || !"INACTIVE".equalsIgnoreCase(employee.getEmploymentStatus());
    }

    public record CreateNotificationRequest(
            Long employeeId,
            String title,
            String message,
            String type,
            Boolean sendEmail,
            Boolean sendSms
    ) {}

    public record BroadcastNotificationRequest(
            String title,
            String message,
            String type,
            String department,
            Boolean sendEmail,
            Boolean sendSms
    ) {}
}
