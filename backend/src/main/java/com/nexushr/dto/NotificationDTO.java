package com.nexushr.dto;

import java.time.LocalDateTime;

public class NotificationDTO {

    private Long id;
    private Long employeeId;
    private String title;
    private String message;
    private String type;
    private String senderUsername;
    private String senderRole;
    private boolean read;
    private LocalDateTime createdAt;

    public NotificationDTO() {
    }

    public NotificationDTO(
            Long id,
            Long employeeId,
            String title,
            String message,
            String type,
            boolean read,
            LocalDateTime createdAt
    ) {
        this(id, employeeId, title, message, type, "SYSTEM", "SYSTEM", read, createdAt);
    }

    public NotificationDTO(
            Long id,
            Long employeeId,
            String title,
            String message,
            String type,
            String senderUsername,
            String senderRole,
            boolean read,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.employeeId = employeeId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.senderUsername = senderUsername;
        this.senderRole = senderRole;
        this.read = read;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getSenderUsername() { return senderUsername; }
    public void setSenderUsername(String senderUsername) { this.senderUsername = senderUsername; }
    public String getSenderRole() { return senderRole; }
    public void setSenderRole(String senderRole) { this.senderRole = senderRole; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
