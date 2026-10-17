package com.nexushr.service;

import com.nexushr.dto.DashboardStatsDTO;
import com.nexushr.repository.AttendanceRepository;
import com.nexushr.repository.EmployeeRepository;
import com.nexushr.repository.LeaveRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private LeaveRepository leaveRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void getDashboardStats_shouldReturnCorrectDashboardStatistics() {
        when(employeeRepository.count())
                .thenReturn(10L);

        when(employeeRepository.countByEmploymentStatus("ACTIVE"))
                .thenReturn(7L);

        when(attendanceRepository
                .countByAttendanceDateAndStatus(
                        LocalDate.now(),
                        "PRESENT"
                ))
                .thenReturn(5L);

        when(attendanceRepository
                .countByAttendanceDateAndStatus(
                        LocalDate.now(),
                        "ABSENT"
                ))
                .thenReturn(2L);

        when(leaveRepository.countByStatus("PENDING"))
                .thenReturn(3L);

        when(leaveRepository.countByStatus("APPROVED"))
                .thenReturn(4L);

        when(employeeRepository.countEmployeesByDepartment())
                .thenReturn(List.of(
                        new Object[]{"IT", 4L},
                        new Object[]{"HR", 3L},
                        new Object[]{"Finance", 3L}
                ));

        DashboardStatsDTO result =
                dashboardService.getDashboardStats();

        assertNotNull(result);

        assertEquals(10L, result.getTotalEmployees());
        assertEquals(7L, result.getActiveEmployees());
        assertEquals(3L, result.getInactiveEmployees());

        assertEquals(5L, result.getTodayPresent());
        assertEquals(2L, result.getTodayAbsent());

        assertEquals(3L, result.getPendingLeaves());
        assertEquals(4L, result.getApprovedLeaves());

        assertNotNull(result.getDepartmentStats());
        assertEquals(3, result.getDepartmentStats().size());

        assertEquals(
                4L,
                result.getDepartmentStats().get("IT")
        );

        assertEquals(
                3L,
                result.getDepartmentStats().get("HR")
        );

        assertEquals(
                3L,
                result.getDepartmentStats().get("Finance")
        );

        verify(employeeRepository).count();
        verify(employeeRepository)
                .countByEmploymentStatus("ACTIVE");

        verify(attendanceRepository)
                .countByAttendanceDateAndStatus(
                        LocalDate.now(),
                        "PRESENT"
                );

        verify(attendanceRepository)
                .countByAttendanceDateAndStatus(
                        LocalDate.now(),
                        "ABSENT"
                );

        verify(leaveRepository)
                .countByStatus("PENDING");

        verify(leaveRepository)
                .countByStatus("APPROVED");

        verify(employeeRepository)
                .countEmployeesByDepartment();
    }

    @Test
    void getDashboardStats_shouldReturnZeroInactiveEmployeesWhenAllAreActive() {
        when(employeeRepository.count())
                .thenReturn(5L);

        when(employeeRepository.countByEmploymentStatus("ACTIVE"))
                .thenReturn(5L);

        when(attendanceRepository
                .countByAttendanceDateAndStatus(
                        LocalDate.now(),
                        "PRESENT"
                ))
                .thenReturn(5L);

        when(attendanceRepository
                .countByAttendanceDateAndStatus(
                        LocalDate.now(),
                        "ABSENT"
                ))
                .thenReturn(0L);

        when(leaveRepository.countByStatus("PENDING"))
                .thenReturn(0L);

        when(leaveRepository.countByStatus("APPROVED"))
                .thenReturn(0L);

        when(employeeRepository.countEmployeesByDepartment())
                .thenReturn(List.of());

        DashboardStatsDTO result =
                dashboardService.getDashboardStats();

        assertEquals(5L, result.getTotalEmployees());
        assertEquals(5L, result.getActiveEmployees());
        assertEquals(0L, result.getInactiveEmployees());
        assertEquals(5L, result.getTodayPresent());
        assertEquals(0L, result.getTodayAbsent());
        assertEquals(0L, result.getPendingLeaves());
        assertEquals(0L, result.getApprovedLeaves());
        assertNotNull(result.getDepartmentStats());
        assertTrue(result.getDepartmentStats().isEmpty());
    }

    @Test
    void getDashboardStats_shouldHandleNoEmployees() {
        when(employeeRepository.count())
                .thenReturn(0L);

        when(employeeRepository.countByEmploymentStatus("ACTIVE"))
                .thenReturn(0L);

        when(attendanceRepository
                .countByAttendanceDateAndStatus(
                        LocalDate.now(),
                        "PRESENT"
                ))
                .thenReturn(0L);

        when(attendanceRepository
                .countByAttendanceDateAndStatus(
                        LocalDate.now(),
                        "ABSENT"
                ))
                .thenReturn(0L);

        when(leaveRepository.countByStatus("PENDING"))
                .thenReturn(0L);

        when(leaveRepository.countByStatus("APPROVED"))
                .thenReturn(0L);

        when(employeeRepository.countEmployeesByDepartment())
                .thenReturn(List.of());

        DashboardStatsDTO result =
                dashboardService.getDashboardStats();

        assertNotNull(result);
        assertEquals(0L, result.getTotalEmployees());
        assertEquals(0L, result.getActiveEmployees());
        assertEquals(0L, result.getInactiveEmployees());
        assertTrue(result.getDepartmentStats().isEmpty());
    }

    @Test
    void getDashboardStats_shouldPreserveDepartmentInsertionOrder() {
        when(employeeRepository.count())
                .thenReturn(3L);

        when(employeeRepository.countByEmploymentStatus("ACTIVE"))
                .thenReturn(2L);

        when(attendanceRepository
                .countByAttendanceDateAndStatus(
                        LocalDate.now(),
                        "PRESENT"
                ))
                .thenReturn(1L);

        when(attendanceRepository
                .countByAttendanceDateAndStatus(
                        LocalDate.now(),
                        "ABSENT"
                ))
                .thenReturn(1L);

        when(leaveRepository.countByStatus("PENDING"))
                .thenReturn(1L);

        when(leaveRepository.countByStatus("APPROVED"))
                .thenReturn(1L);

        when(employeeRepository.countEmployeesByDepartment())
                .thenReturn(List.of(
                        new Object[]{"Finance", 1L},
                        new Object[]{"IT", 1L},
                        new Object[]{"HR", 1L}
                ));

        DashboardStatsDTO result =
                dashboardService.getDashboardStats();

        assertEquals(
                List.of("Finance", "IT", "HR"),
                List.copyOf(
                        result.getDepartmentStats().keySet()
                )
        );
    }
}
