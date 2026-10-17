package com.nexushr.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "performance_reviews")
public class PerformanceReview {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false)
    private LocalDate reviewPeriodStart;

    @Column(nullable = false)
    private LocalDate reviewPeriodEnd;

    @Column(nullable = false)
    private Integer rating;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewStatus status;

    private String reviewCycle;
    private String reviewType;

    @Column(precision = 5, scale = 2)
    private BigDecimal competencyScore;

    @Column(precision = 5, scale = 2)
    private BigDecimal goalScore;

    @Column(precision = 5, scale = 2)
    private BigDecimal finalScore;

    private LocalDateTime submittedAt;
    private LocalDateTime completedAt;

    @Column(columnDefinition = "TEXT") private String goals;
    @Column(columnDefinition = "TEXT") private String strengths;
    @Column(columnDefinition = "TEXT") private String areasForImprovement;
    @Column(columnDefinition = "TEXT") private String managerComments;
    @Column(columnDefinition = "TEXT") private String employeeComments;

    public enum ReviewStatus { DRAFT, SELF_REVIEW, SUBMITTED, MANAGER_REVIEW, COMPLETED }

    public PerformanceReview() {}

    @PrePersist
    public void prePersist() {
        if (status == null) status = ReviewStatus.DRAFT;
        if (reviewType == null || reviewType.isBlank()) reviewType = "ANNUAL";
    }

    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public Employee getEmployee(){return employee;} public void setEmployee(Employee v){this.employee=v;}
    public LocalDate getReviewPeriodStart(){return reviewPeriodStart;} public void setReviewPeriodStart(LocalDate v){this.reviewPeriodStart=v;}
    public LocalDate getReviewPeriodEnd(){return reviewPeriodEnd;} public void setReviewPeriodEnd(LocalDate v){this.reviewPeriodEnd=v;}
    public Integer getRating(){return rating;} public void setRating(Integer v){this.rating=v;}
    public ReviewStatus getStatus(){return status;} public void setStatus(ReviewStatus v){this.status=v;}
    public String getReviewCycle(){return reviewCycle;} public void setReviewCycle(String v){this.reviewCycle=v;}
    public String getReviewType(){return reviewType;} public void setReviewType(String v){this.reviewType=v;}
    public BigDecimal getCompetencyScore(){return competencyScore;} public void setCompetencyScore(BigDecimal v){this.competencyScore=v;}
    public BigDecimal getGoalScore(){return goalScore;} public void setGoalScore(BigDecimal v){this.goalScore=v;}
    public BigDecimal getFinalScore(){return finalScore;} public void setFinalScore(BigDecimal v){this.finalScore=v;}
    public LocalDateTime getSubmittedAt(){return submittedAt;} public void setSubmittedAt(LocalDateTime v){this.submittedAt=v;}
    public LocalDateTime getCompletedAt(){return completedAt;} public void setCompletedAt(LocalDateTime v){this.completedAt=v;}
    public String getGoals(){return goals;} public void setGoals(String v){this.goals=v;}
    public String getStrengths(){return strengths;} public void setStrengths(String v){this.strengths=v;}
    public String getAreasForImprovement(){return areasForImprovement;} public void setAreasForImprovement(String v){this.areasForImprovement=v;}
    public String getManagerComments(){return managerComments;} public void setManagerComments(String v){this.managerComments=v;}
    public String getEmployeeComments(){return employeeComments;} public void setEmployeeComments(String v){this.employeeComments=v;}
}
