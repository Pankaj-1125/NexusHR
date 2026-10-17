package com.nexushr.service;

import com.nexushr.dto.WorkforceIntelligenceDTO;
import com.nexushr.entity.Attendance;
import com.nexushr.entity.Employee;
import com.nexushr.entity.Leave;
import com.nexushr.entity.PerformanceReview;

import com.nexushr.repository.AttendanceRepository;
import com.nexushr.repository.EmployeeRepository;
import com.nexushr.repository.LeaveRepository;
import com.nexushr.repository.PerformanceReviewRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkforceIntelligenceServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private LeaveRepository leaveRepository;

    @Mock
    private PerformanceReviewRepository
            performanceReviewRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private WorkforceIntelligenceService
            workforceIntelligenceService;

    private Employee createEmployee() {

        Employee employee =
                new Employee();

        employee.setId(1L);
        employee.setFirstName("John");
        employee.setLastName("Doe");
        employee.setEmail("john@example.com");
        employee.setDepartment("Engineering");
        employee.setJobTitle("Software Developer");
        employee.setEmploymentStatus("ACTIVE");
        employee.setJoiningDate(
                LocalDate.now().minusMonths(12)
        );

        return employee;
    }

    @Test
    void shouldAnalyzeEmployeeAndAudit() {

        Employee employee =
                createEmployee();

        when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(employee));

        when(attendanceRepository.findByEmployeeId(1L))
                .thenReturn(List.of());

        when(leaveRepository.findByEmployeeId(1L))
                .thenReturn(List.of());

        when(performanceReviewRepository.findByEmployeeId(1L))
                .thenReturn(List.of());

        WorkforceIntelligenceDTO result =
                workforceIntelligenceService
                        .analyzeEmployee(1L);

        assertNotNull(result);
        assertEquals(1L, result.getEmployeeId());
        assertEquals(
                "John Doe",
                result.getEmployeeName()
        );
        assertEquals(
                "Engineering",
                result.getDepartment()
        );

        assertTrue(
                result.getAttritionRiskScore() >= 0
        );

        assertTrue(
                result.getAttritionRiskScore() <= 100
        );

        assertTrue(
                result.getEngagementScore() >= 0
        );

        assertTrue(
                result.getEngagementScore() <= 100
        );

        verify(auditLogService)
                .log(
                        eq("WORKFORCE_INTELLIGENCE_ANALYZED"),
                        eq("EMPLOYEE"),
                        eq(1L),
                        eq(
                                "Workforce intelligence analysis generated"
                        )
                );
    }

    @Test
    void shouldRejectUnknownEmployee() {

        when(employeeRepository.findById(999L))
                .thenReturn(Optional.empty());

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () ->
                                workforceIntelligenceService
                                        .analyzeEmployee(999L)
                );

        assertEquals(
                "Employee not found with ID: 999",
                exception.getMessage()
        );

        verifyNoInteractions(auditLogService);
    }

    @Test
    void shouldDetectFrequentAbsenceRisk() {

        Employee employee =
                createEmployee();

        when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(employee));

        Attendance attendance =
                new Attendance();

        attendance.setStatus("ABSENT");

        Attendance attendance2 =
                new Attendance();

        attendance2.setStatus("ABSENT");

        Attendance attendance3 =
                new Attendance();

        attendance3.setStatus("ABSENT");

        Attendance attendance4 =
                new Attendance();

        attendance4.setStatus("ABSENT");

        Attendance attendance5 =
                new Attendance();

        attendance5.setStatus("ABSENT");

        when(attendanceRepository.findByEmployeeId(1L))
                .thenReturn(
                        List.of(
                                attendance,
                                attendance2,
                                attendance3,
                                attendance4,
                                attendance5
                        )
                );

        when(leaveRepository.findByEmployeeId(1L))
                .thenReturn(List.of());

        when(performanceReviewRepository.findByEmployeeId(1L))
                .thenReturn(List.of());

        WorkforceIntelligenceDTO result =
                workforceIntelligenceService
                        .analyzeEmployee(1L);

        assertTrue(
                result.getAttritionRiskScore() >= 20
        );

        assertTrue(
                result.getRiskFactors()
                        .contains(
                                "Frequent absence detected"
                        )
        );
    }

    @Test
    void shouldDetectLowPerformanceRisk() {

        Employee employee =
                createEmployee();

        when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(employee));

        when(attendanceRepository.findByEmployeeId(1L))
                .thenReturn(List.of());

        when(leaveRepository.findByEmployeeId(1L))
                .thenReturn(List.of());

        PerformanceReview review =
                new PerformanceReview();

        review.setRating(1);

        when(
                performanceReviewRepository
                        .findByEmployeeId(1L)
        ).thenReturn(List.of(review));

        WorkforceIntelligenceDTO result =
                workforceIntelligenceService
                        .analyzeEmployee(1L);

        assertTrue(
                result.getAttritionRiskScore() >= 20
        );

        assertTrue(
                result.getRiskFactors()
                        .contains(
                                "Low recent performance rating"
                        )
        );

        assertTrue(
                result.getSkillGapIndicators()
                        .contains(
                                "Performance improvement required"
                        )
        );
    }

    @Test
    void shouldDetectMultiplePendingLeaves() {

        Employee employee =
                createEmployee();

        when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(employee));

        when(attendanceRepository.findByEmployeeId(1L))
                .thenReturn(List.of());

        Leave leave1 =
                new Leave();

        leave1.setStatus("PENDING");
        leave1.setNumberOfDays(2);

        Leave leave2 =
                new Leave();

        leave2.setStatus("PENDING");
        leave2.setNumberOfDays(1);

        Leave leave3 =
                new Leave();

        leave3.setStatus("PENDING");
        leave3.setNumberOfDays(2);

        when(leaveRepository.findByEmployeeId(1L))
                .thenReturn(
                        List.of(
                                leave1,
                                leave2,
                                leave3
                        )
                );

        when(performanceReviewRepository.findByEmployeeId(1L))
                .thenReturn(List.of());

        WorkforceIntelligenceDTO result =
                workforceIntelligenceService
                        .analyzeEmployee(1L);

        assertTrue(
                result.getRiskFactors()
                        .contains(
                                "Multiple pending leave requests"
                        )
        );
    }

    @Test
    void shouldAnalyzeAllActiveEmployeesAndAuditOnce() {

        Employee employee1 =
                createEmployee();

        Employee employee2 =
                createEmployee();

        employee2.setId(2L);
        employee2.setFirstName("Jane");
        employee2.setLastName("Smith");

        Employee inactive =
                createEmployee();

        inactive.setId(3L);
        inactive.setEmploymentStatus("INACTIVE");

        when(employeeRepository.findAll())
                .thenReturn(
                        List.of(
                                employee1,
                                employee2,
                                inactive
                        )
                );

        when(attendanceRepository.findByEmployeeId(anyLong()))
                .thenReturn(List.of());

        when(leaveRepository.findByEmployeeId(anyLong()))
                .thenReturn(List.of());

        when(
                performanceReviewRepository
                        .findByEmployeeId(anyLong())
        ).thenReturn(List.of());

        List<WorkforceIntelligenceDTO> result =
                workforceIntelligenceService
                        .analyzeAllEmployees();

        assertEquals(2, result.size());

        assertTrue(
                result.stream()
                        .noneMatch(dto ->
                                dto.getEmployeeId().equals(3L)
                        )
        );

        verify(auditLogService)
                .log(
                        eq(
                                "WORKFORCE_INTELLIGENCE_ANALYZED_ALL"
                        ),
                        eq("WORKFORCE"),
                        eq(0L),
                        contains(
                                "2 active employee(s)"
                        )
                );

        verify(
                auditLogService,
                times(1)
        ).log(
                anyString(),
                anyString(),
                anyLong(),
                anyString()
        );
    }
}