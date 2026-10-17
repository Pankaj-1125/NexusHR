package com.nexushr.dto;

import java.util.List;

public class WorkforceIntelligenceDTO {

    private Long employeeId;
    private String employeeName;
    private String department;
    private String jobTitle;
    private long tenureMonths;

    private int attritionRiskScore;
    private String attritionRiskLevel;
    private int retentionScore;

    private int engagementScore;
    private String engagementLevel;

    private int burnoutRiskScore;
    private String burnoutRiskLevel;

    private int growthPotentialScore;
    private String growthPotentialLevel;

    private int skillGapScore;
    private String skillGapLevel;

    private double attendanceRate;
    private long absentDays;
    private long attendanceRecords;
    private double averageWorkingHours;

    private long pendingLeaveRequests;
    private long approvedLeaveDays;

    private double averagePerformanceRating;
    private String performanceTrend;
    private int performanceReviewCount;

    private int confidenceScore;
    private String dataQuality;
    private String priority;
    private String insightSummary;
    private String modelVersion;

    private List<String> riskFactors;
    private List<String> skillGapIndicators;
    private List<String> recommendations;
    private List<String> strengths;

    public WorkforceIntelligenceDTO() {
    }

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    public long getTenureMonths() { return tenureMonths; }
    public void setTenureMonths(long tenureMonths) { this.tenureMonths = tenureMonths; }
    public int getAttritionRiskScore() { return attritionRiskScore; }
    public void setAttritionRiskScore(int attritionRiskScore) { this.attritionRiskScore = attritionRiskScore; }
    public String getAttritionRiskLevel() { return attritionRiskLevel; }
    public void setAttritionRiskLevel(String attritionRiskLevel) { this.attritionRiskLevel = attritionRiskLevel; }
    public int getRetentionScore() { return retentionScore; }
    public void setRetentionScore(int retentionScore) { this.retentionScore = retentionScore; }
    public int getEngagementScore() { return engagementScore; }
    public void setEngagementScore(int engagementScore) { this.engagementScore = engagementScore; }
    public String getEngagementLevel() { return engagementLevel; }
    public void setEngagementLevel(String engagementLevel) { this.engagementLevel = engagementLevel; }
    public int getBurnoutRiskScore() { return burnoutRiskScore; }
    public void setBurnoutRiskScore(int burnoutRiskScore) { this.burnoutRiskScore = burnoutRiskScore; }
    public String getBurnoutRiskLevel() { return burnoutRiskLevel; }
    public void setBurnoutRiskLevel(String burnoutRiskLevel) { this.burnoutRiskLevel = burnoutRiskLevel; }
    public int getGrowthPotentialScore() { return growthPotentialScore; }
    public void setGrowthPotentialScore(int growthPotentialScore) { this.growthPotentialScore = growthPotentialScore; }
    public String getGrowthPotentialLevel() { return growthPotentialLevel; }
    public void setGrowthPotentialLevel(String growthPotentialLevel) { this.growthPotentialLevel = growthPotentialLevel; }
    public int getSkillGapScore() { return skillGapScore; }
    public void setSkillGapScore(int skillGapScore) { this.skillGapScore = skillGapScore; }
    public String getSkillGapLevel() { return skillGapLevel; }
    public void setSkillGapLevel(String skillGapLevel) { this.skillGapLevel = skillGapLevel; }
    public double getAttendanceRate() { return attendanceRate; }
    public void setAttendanceRate(double attendanceRate) { this.attendanceRate = attendanceRate; }
    public long getAbsentDays() { return absentDays; }
    public void setAbsentDays(long absentDays) { this.absentDays = absentDays; }
    public long getAttendanceRecords() { return attendanceRecords; }
    public void setAttendanceRecords(long attendanceRecords) { this.attendanceRecords = attendanceRecords; }
    public double getAverageWorkingHours() { return averageWorkingHours; }
    public void setAverageWorkingHours(double averageWorkingHours) { this.averageWorkingHours = averageWorkingHours; }
    public long getPendingLeaveRequests() { return pendingLeaveRequests; }
    public void setPendingLeaveRequests(long pendingLeaveRequests) { this.pendingLeaveRequests = pendingLeaveRequests; }
    public long getApprovedLeaveDays() { return approvedLeaveDays; }
    public void setApprovedLeaveDays(long approvedLeaveDays) { this.approvedLeaveDays = approvedLeaveDays; }
    public double getAveragePerformanceRating() { return averagePerformanceRating; }
    public void setAveragePerformanceRating(double averagePerformanceRating) { this.averagePerformanceRating = averagePerformanceRating; }
    public String getPerformanceTrend() { return performanceTrend; }
    public void setPerformanceTrend(String performanceTrend) { this.performanceTrend = performanceTrend; }
    public int getPerformanceReviewCount() { return performanceReviewCount; }
    public void setPerformanceReviewCount(int performanceReviewCount) { this.performanceReviewCount = performanceReviewCount; }
    public int getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(int confidenceScore) { this.confidenceScore = confidenceScore; }
    public String getDataQuality() { return dataQuality; }
    public void setDataQuality(String dataQuality) { this.dataQuality = dataQuality; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getInsightSummary() { return insightSummary; }
    public void setInsightSummary(String insightSummary) { this.insightSummary = insightSummary; }
    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
    public List<String> getRiskFactors() { return riskFactors; }
    public void setRiskFactors(List<String> riskFactors) { this.riskFactors = riskFactors; }
    public List<String> getSkillGapIndicators() { return skillGapIndicators; }
    public void setSkillGapIndicators(List<String> skillGapIndicators) { this.skillGapIndicators = skillGapIndicators; }
    public List<String> getRecommendations() { return recommendations; }
    public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
    public List<String> getStrengths() { return strengths; }
    public void setStrengths(List<String> strengths) { this.strengths = strengths; }
}
