package com.nexushr.dto;

import java.util.Map;

public class ReportSummaryDTO {

    private long totalEmployees;
    private long activeEmployees;
    private long inactiveEmployees;

    private long todayPresent;
    private long todayAbsent;

    private long pendingLeaves;
    private long approvedLeaves;

    private long totalPayrollRecords;
    private long draftPayrolls;
    private long processedPayrolls;
    private long paidPayrolls;

    private long totalPerformanceReviews;
    private long draftPerformanceReviews;
    private long submittedPerformanceReviews;
    private long completedPerformanceReviews;
    private double averagePerformanceRating;

    private Map<String, Long> departmentStats;

    public ReportSummaryDTO() {
    }

    public long getTotalEmployees() {
        return totalEmployees;
    }

    public void setTotalEmployees(long totalEmployees) {
        this.totalEmployees = totalEmployees;
    }

    public long getActiveEmployees() {
        return activeEmployees;
    }

    public void setActiveEmployees(long activeEmployees) {
        this.activeEmployees = activeEmployees;
    }

    public long getInactiveEmployees() {
        return inactiveEmployees;
    }

    public void setInactiveEmployees(long inactiveEmployees) {
        this.inactiveEmployees = inactiveEmployees;
    }

    public long getTodayPresent() {
        return todayPresent;
    }

    public void setTodayPresent(long todayPresent) {
        this.todayPresent = todayPresent;
    }

    public long getTodayAbsent() {
        return todayAbsent;
    }

    public void setTodayAbsent(long todayAbsent) {
        this.todayAbsent = todayAbsent;
    }

    public long getPendingLeaves() {
        return pendingLeaves;
    }

    public void setPendingLeaves(long pendingLeaves) {
        this.pendingLeaves = pendingLeaves;
    }

    public long getApprovedLeaves() {
        return approvedLeaves;
    }

    public void setApprovedLeaves(long approvedLeaves) {
        this.approvedLeaves = approvedLeaves;
    }

    public long getTotalPayrollRecords() {
        return totalPayrollRecords;
    }

    public void setTotalPayrollRecords(long totalPayrollRecords) {
        this.totalPayrollRecords = totalPayrollRecords;
    }

    public long getDraftPayrolls() {
        return draftPayrolls;
    }

    public void setDraftPayrolls(long draftPayrolls) {
        this.draftPayrolls = draftPayrolls;
    }

    public long getProcessedPayrolls() {
        return processedPayrolls;
    }

    public void setProcessedPayrolls(long processedPayrolls) {
        this.processedPayrolls = processedPayrolls;
    }

    public long getPaidPayrolls() {
        return paidPayrolls;
    }

    public void setPaidPayrolls(long paidPayrolls) {
        this.paidPayrolls = paidPayrolls;
    }

    public long getTotalPerformanceReviews() {
        return totalPerformanceReviews;
    }

    public void setTotalPerformanceReviews(long totalPerformanceReviews) {
        this.totalPerformanceReviews = totalPerformanceReviews;
    }

    public long getDraftPerformanceReviews() {
        return draftPerformanceReviews;
    }

    public void setDraftPerformanceReviews(long draftPerformanceReviews) {
        this.draftPerformanceReviews = draftPerformanceReviews;
    }

    public long getSubmittedPerformanceReviews() {
        return submittedPerformanceReviews;
    }

    public void setSubmittedPerformanceReviews(long submittedPerformanceReviews) {
        this.submittedPerformanceReviews = submittedPerformanceReviews;
    }

    public long getCompletedPerformanceReviews() {
        return completedPerformanceReviews;
    }

    public void setCompletedPerformanceReviews(long completedPerformanceReviews) {
        this.completedPerformanceReviews = completedPerformanceReviews;
    }

    public double getAveragePerformanceRating() {
        return averagePerformanceRating;
    }

    public void setAveragePerformanceRating(double averagePerformanceRating) {
        this.averagePerformanceRating = averagePerformanceRating;
    }

    public Map<String, Long> getDepartmentStats() {
        return departmentStats;
    }

    public void setDepartmentStats(Map<String, Long> departmentStats) {
        this.departmentStats = departmentStats;
    }
}
