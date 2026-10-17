package com.nexushr.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 2000)
    private String message;

    @Column(nullable = false)
    private String type;

    @Column(name = "sender_username", length = 100)
    private String senderUsername;

    @Column(name = "sender_role", length = 50)
    private String senderRole;

    @Column(nullable = false)
    private boolean read = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public Notification() {
    }

    public Notification(
            Long employeeId,
            String title,
            String message,
            String type
    ) {
        this(employeeId, title, message, type, "SYSTEM", "SYSTEM");
    }

    public Notification(
            Long employeeId,
            String title,
            String message,
            String type,
            String senderUsername,
            String senderRole
    ) {
        this.employeeId = employeeId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.senderUsername = senderUsername;
        this.senderRole = senderRole;
        this.read = false;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (senderUsername == null || senderUsername.isBlank()) {
            senderUsername = "SYSTEM";
        }
        if (senderRole == null || senderRole.isBlank()) {
            senderRole = "SYSTEM";
        }
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
