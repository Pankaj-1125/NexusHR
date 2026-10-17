package com.nexushr.dto;

import java.util.Map;

public class DashboardStatsDTO {

    private long totalEmployees;
    private long activeEmployees;
    private long inactiveEmployees;

    private long todayPresent;
    private long todayAbsent;

    private long pendingLeaves;
    private long approvedLeaves;

    private Map<String, Long> departmentStats;

    public DashboardStatsDTO() {
    }

    public DashboardStatsDTO(
            long totalEmployees,
            long activeEmployees,
            long inactiveEmployees,
            long todayPresent,
            long todayAbsent,
            long pendingLeaves,
            long approvedLeaves,
            Map<String, Long> departmentStats) {

        this.totalEmployees = totalEmployees;
        this.activeEmployees = activeEmployees;
        this.inactiveEmployees = inactiveEmployees;
        this.todayPresent = todayPresent;
        this.todayAbsent = todayAbsent;
        this.pendingLeaves = pendingLeaves;
        this.approvedLeaves = approvedLeaves;
        this.departmentStats = departmentStats;
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

    public Map<String, Long> getDepartmentStats() {
        return departmentStats;
    }

    public void setDepartmentStats(Map<String, Long> departmentStats) {
        this.departmentStats = departmentStats;
    }
}