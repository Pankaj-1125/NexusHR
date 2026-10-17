package com.nexushr.repository;

import com.nexushr.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository
        extends JpaRepository<Attendance, Long> {

    List<Attendance> findByAttendanceDate(
            LocalDate attendanceDate
    );

    List<Attendance> findByEmployeeId(
            Long employeeId
    );

    Optional<Attendance> findByEmployeeIdAndAttendanceDate(
            Long employeeId,
            LocalDate attendanceDate
    );

    List<Attendance> findByEmployeeIdAndAttendanceDateBetween(
            Long employeeId,
            LocalDate startDate,
            LocalDate endDate
    );

    List<Attendance> findByStatus(
            String status
    );

    long countByAttendanceDateAndStatus(
            LocalDate attendanceDate,
            String status
    );
}