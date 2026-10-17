package com.nexushr.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "job_applications")
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(nullable = false)
    private String applicantName;

    @Column(nullable = false)
    private String email;

    private String phone;

    private String resumeUrl;

    @Column(columnDefinition = "TEXT")
    private String coverLetter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status;

    private LocalDate appliedDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private String qualification;

    @Column(precision = 5, scale = 2)
    private BigDecimal experienceYears;

    @Column(columnDefinition = "TEXT")
    private String skills;

    private String currentCity;

    private String linkedinUrl;

    private String portfolioUrl;

    @Column(precision = 12, scale = 2)
    private BigDecimal expectedSalary;

    private String noticePeriod;

    private String source;

    private String applicationReference;

    private String managerShortlistedBy;

    private LocalDateTime managerShortlistedAt;

    private String adminApprovedBy;

    private LocalDateTime adminApprovedAt;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(columnDefinition = "TEXT")
    private String recruiterNotes;

    @Column(columnDefinition = "TEXT")
    private String finalSelectionMessage;

    @Column(columnDefinition = "TEXT")
    private String requiredDocuments;

    @Column(columnDefinition = "TEXT")
    private String reportingInstructions;

    private String finalApprovedBy;

    private LocalDateTime finalApprovedAt;

    public JobApplication() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public void setApplicantName(String applicantName) {
        this.applicantName = applicantName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getResumeUrl() {
        return resumeUrl;
    }

    public void setResumeUrl(String resumeUrl) {
        this.resumeUrl = resumeUrl;
    }

    public String getCoverLetter() {
        return coverLetter;
    }

    public void setCoverLetter(String coverLetter) {
        this.coverLetter = coverLetter;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public LocalDate getAppliedDate() {
        return appliedDate;
    }

    public void setAppliedDate(LocalDate appliedDate) {
        this.appliedDate = appliedDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getQualification() { return qualification; }
    public void setQualification(String qualification) { this.qualification = qualification; }
    public BigDecimal getExperienceYears() { return experienceYears; }
    public void setExperienceYears(BigDecimal experienceYears) { this.experienceYears = experienceYears; }
    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }
    public String getCurrentCity() { return currentCity; }
    public void setCurrentCity(String currentCity) { this.currentCity = currentCity; }
    public String getLinkedinUrl() { return linkedinUrl; }
    public void setLinkedinUrl(String linkedinUrl) { this.linkedinUrl = linkedinUrl; }
    public String getPortfolioUrl() { return portfolioUrl; }
    public void setPortfolioUrl(String portfolioUrl) { this.portfolioUrl = portfolioUrl; }
    public BigDecimal getExpectedSalary() { return expectedSalary; }
    public void setExpectedSalary(BigDecimal expectedSalary) { this.expectedSalary = expectedSalary; }
    public String getNoticePeriod() { return noticePeriod; }
    public void setNoticePeriod(String noticePeriod) { this.noticePeriod = noticePeriod; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getApplicationReference() { return applicationReference; }
    public void setApplicationReference(String applicationReference) { this.applicationReference = applicationReference; }
    public String getManagerShortlistedBy() { return managerShortlistedBy; }
    public void setManagerShortlistedBy(String managerShortlistedBy) { this.managerShortlistedBy = managerShortlistedBy; }
    public LocalDateTime getManagerShortlistedAt() { return managerShortlistedAt; }
    public void setManagerShortlistedAt(LocalDateTime managerShortlistedAt) { this.managerShortlistedAt = managerShortlistedAt; }
    public String getAdminApprovedBy() { return adminApprovedBy; }
    public void setAdminApprovedBy(String adminApprovedBy) { this.adminApprovedBy = adminApprovedBy; }
    public LocalDateTime getAdminApprovedAt() { return adminApprovedAt; }
    public void setAdminApprovedAt(LocalDateTime adminApprovedAt) { this.adminApprovedAt = adminApprovedAt; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public String getRecruiterNotes() { return recruiterNotes; }
    public void setRecruiterNotes(String recruiterNotes) { this.recruiterNotes = recruiterNotes; }
    public String getFinalSelectionMessage() { return finalSelectionMessage; }
    public void setFinalSelectionMessage(String finalSelectionMessage) { this.finalSelectionMessage = finalSelectionMessage; }
    public String getRequiredDocuments() { return requiredDocuments; }
    public void setRequiredDocuments(String requiredDocuments) { this.requiredDocuments = requiredDocuments; }
    public String getReportingInstructions() { return reportingInstructions; }
    public void setReportingInstructions(String reportingInstructions) { this.reportingInstructions = reportingInstructions; }
    public String getFinalApprovedBy() { return finalApprovedBy; }
    public void setFinalApprovedBy(String finalApprovedBy) { this.finalApprovedBy = finalApprovedBy; }
    public LocalDateTime getFinalApprovedAt() { return finalApprovedAt; }
    public void setFinalApprovedAt(LocalDateTime finalApprovedAt) { this.finalApprovedAt = finalApprovedAt; }

    public enum ApplicationStatus {
        APPLIED,
        SCREENING,
        MANAGER_SHORTLISTED,
        ADMIN_APPROVED,
        INTERVIEW,
        INTERVIEW_COMPLETED,
        OFFER,
        HIRED,
        REJECTED
    }
}