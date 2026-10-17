package com.nexushr.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "performance_goals")
public class PerformanceGoal {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT") private String description;
    @Column(columnDefinition = "TEXT") private String successMetric;
    @Column(nullable = false) private LocalDate startDate;
    @Column(nullable = false) private LocalDate targetDate;
    @Column(nullable = false) private Integer progress;
    @Column(nullable = false) private Integer weight;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GoalStatus status;

    @Column(columnDefinition = "TEXT") private String managerComment;
    @Column(columnDefinition = "TEXT") private String employeeComment;
    @Column(nullable = false) private LocalDateTime createdAt;
    @Column(nullable = false) private LocalDateTime updatedAt;

    public enum GoalStatus { DRAFT, ACTIVE, COMPLETED, CANCELLED }

    public PerformanceGoal() {}

    @PrePersist public void prePersist() {
        if (progress == null) progress = 0;
        if (weight == null) weight = 100;
        if (status == null) status = GoalStatus.DRAFT;
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }
    @PreUpdate public void preUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getId(){return id;} public void setId(Long v){id=v;}
    public Employee getEmployee(){return employee;} public void setEmployee(Employee v){employee=v;}
    public String getTitle(){return title;} public void setTitle(String v){title=v;}
    public String getDescription(){return description;} public void setDescription(String v){description=v;}
    public String getSuccessMetric(){return successMetric;} public void setSuccessMetric(String v){successMetric=v;}
    public LocalDate getStartDate(){return startDate;} public void setStartDate(LocalDate v){startDate=v;}
    public LocalDate getTargetDate(){return targetDate;} public void setTargetDate(LocalDate v){targetDate=v;}
    public Integer getProgress(){return progress;} public void setProgress(Integer v){progress=v;}
    public Integer getWeight(){return weight;} public void setWeight(Integer v){weight=v;}
    public GoalStatus getStatus(){return status;} public void setStatus(GoalStatus v){status=v;}
    public String getManagerComment(){return managerComment;} public void setManagerComment(String v){managerComment=v;}
    public String getEmployeeComment(){return employeeComment;} public void setEmployeeComment(String v){employeeComment=v;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;}
    public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime v){updatedAt=v;}
}
