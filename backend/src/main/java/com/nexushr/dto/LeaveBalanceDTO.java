package com.nexushr.dto;

public class LeaveBalanceDTO {

    private Long employeeId;
    private int year;
    private String leaveType;
    private int annualEntitlement;
    private int approvedDays;
    private int pendingDays;
    private int remainingDays;

    public LeaveBalanceDTO() {
    }

    public LeaveBalanceDTO(
            Long employeeId,
            int year,
            String leaveType,
            int annualEntitlement,
            int approvedDays,
            int pendingDays,
            int remainingDays
    ) {
        this.employeeId = employeeId;
        this.year = year;
        this.leaveType = leaveType;
        this.annualEntitlement = annualEntitlement;
        this.approvedDays = approvedDays;
        this.pendingDays = pendingDays;
        this.remainingDays = remainingDays;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public String getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(String leaveType) {
        this.leaveType = leaveType;
    }

    public int getAnnualEntitlement() {
        return annualEntitlement;
    }

    public void setAnnualEntitlement(int annualEntitlement) {
        this.annualEntitlement = annualEntitlement;
    }

    public int getApprovedDays() {
        return approvedDays;
    }

    public void setApprovedDays(int approvedDays) {
        this.approvedDays = approvedDays;
    }

    public int getPendingDays() {
        return pendingDays;
    }

    public void setPendingDays(int pendingDays) {
        this.pendingDays = pendingDays;
    }

    public int getRemainingDays() {
        return remainingDays;
    }

    public void setRemainingDays(int remainingDays) {
        this.remainingDays = remainingDays;
    }
}