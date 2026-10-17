package com.nexushr.service;

import com.nexushr.dto.NotificationDTO;
import com.nexushr.entity.Employee;
import com.nexushr.repository.EmployeeRepository;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class RealtimeNotificationPublisher {

    private final SimpMessagingTemplate messagingTemplate;
    private final EmployeeRepository employeeRepository;

    public RealtimeNotificationPublisher(
            SimpMessagingTemplate messagingTemplate,
            EmployeeRepository employeeRepository
    ) {
        this.messagingTemplate = messagingTemplate;
        this.employeeRepository = employeeRepository;
    }

    public void publish(
            NotificationDTO notification
    ) {

        if (notification == null
                || notification.getEmployeeId() == null) {
            return;
        }

        Employee employee =
                employeeRepository
                        .findById(
                                notification.getEmployeeId()
                        )
                        .orElse(null);

        if (employee == null
                || employee.getUser() == null
                || employee.getUser().getUsername() == null
                || employee.getUser()
                .getUsername()
                .isBlank()) {
            return;
        }

        String username =
                employee
                        .getUser()
                        .getUsername();

        messagingTemplate.convertAndSendToUser(
                username,
                "/queue/notifications",
                notification
        );
    }
}