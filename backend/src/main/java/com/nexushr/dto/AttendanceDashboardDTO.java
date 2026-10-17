package com.nexushr.dto;

import java.time.LocalDate;

public class AttendanceDashboardDTO {

    private LocalDate date;

    private long totalEmployees;

    private long present;

    private long absent;

    private long checkedIn;

    private long checkedOut;

    public AttendanceDashboardDTO() {
    }

    public AttendanceDashboardDTO(
            LocalDate date,
            long totalEmployees,
            long present,
            long absent,
            long checkedIn,
            long checkedOut) {

        this.date = date;
        this.totalEmployees = totalEmployees;
        this.present = present;
        this.absent = absent;
        this.checkedIn = checkedIn;
        this.checkedOut = checkedOut;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public long getTotalEmployees() {
        return totalEmployees;
    }

    public void setTotalEmployees(long totalEmployees) {
        this.totalEmployees = totalEmployees;
    }

    public long getPresent() {
        return present;
    }

    public void setPresent(long present) {
        this.present = present;
    }

    public long getAbsent() {
        return absent;
    }

    public void setAbsent(long absent) {
        this.absent = absent;
    }

    public long getCheckedIn() {
        return checkedIn;
    }

    public void setCheckedIn(long checkedIn) {
        this.checkedIn = checkedIn;
    }

    public long getCheckedOut() {
        return checkedOut;
    }

    public void setCheckedOut(long checkedOut) {
        this.checkedOut = checkedOut;
    }
}