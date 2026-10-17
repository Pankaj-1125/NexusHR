package com.nexushr.service;

import com.nexushr.dto.DashboardStatsDTO;
import com.nexushr.repository.AttendanceRepository;
import com.nexushr.repository.EmployeeRepository;
import com.nexushr.repository.LeaveRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class DashboardService {

    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveRepository leaveRepository;

    public DashboardService(
            EmployeeRepository employeeRepository,
            AttendanceRepository attendanceRepository,
            LeaveRepository leaveRepository) {

        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
        this.leaveRepository = leaveRepository;
    }

    public DashboardStatsDTO getDashboardStats() {

        long totalEmployees =
                employeeRepository.count();

        long activeEmployees =
                employeeRepository.countByEmploymentStatus("ACTIVE");

        long inactiveEmployees =
                totalEmployees - activeEmployees;

        LocalDate today = LocalDate.now();

        long todayPresent =
                attendanceRepository
                        .countByAttendanceDateAndStatus(
                                today,
                                "PRESENT"
                        );

        long todayAbsent =
                attendanceRepository
                        .countByAttendanceDateAndStatus(
                                today,
                                "ABSENT"
                        );

        long pendingLeaves =
                leaveRepository.countByStatus("PENDING");

        long approvedLeaves =
                leaveRepository.countByStatus("APPROVED");

        Map<String, Long> departmentStats =
                getDepartmentStatistics();

        return new DashboardStatsDTO(
                totalEmployees,
                activeEmployees,
                inactiveEmployees,
                todayPresent,
                todayAbsent,
                pendingLeaves,
                approvedLeaves,
                departmentStats
        );
    }

    private Map<String, Long> getDepartmentStatistics() {

        Map<String, Long> departmentStats =
                new LinkedHashMap<>();

        employeeRepository
                .countEmployeesByDepartment()
                .forEach(result -> {

                    String department = (String) result[0];

                    Long count =
                            ((Number) result[1]).longValue();

                    departmentStats.put(
                            department,
                            count
                    );
                });

        return departmentStats;
    }
}