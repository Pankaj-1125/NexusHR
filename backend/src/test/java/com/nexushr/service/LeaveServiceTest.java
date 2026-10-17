package com.nexushr.service;

import com.nexushr.dto.LeaveBalanceDTO;
import com.nexushr.dto.LeaveDTO;
import com.nexushr.entity.Employee;
import com.nexushr.entity.Leave;
import com.nexushr.repository.EmployeeRepository;
import com.nexushr.repository.LeaveRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;


import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveServiceTest {

    @Mock
    private LeaveRepository leaveRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private LeaveService leaveService;

    // ============================================================
    // SETUP
    // ============================================================

    @BeforeEach
    void setUp() throws Exception {

        lenient().doReturn(true).when(authentication).isAuthenticated();

        lenient().doReturn("admin").when(authentication).getName();

        lenient()
                .doReturn(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .when(authentication)
                .getAuthorities();
        assertNotNull(authentication);
        assertTrue(authentication.isAuthenticated());


        /*
         * LeaveService gets these values from @Value properties.
         * Mockito does not load application.properties,
         * therefore they must be initialized manually.
         */

        Field casualField =
                LeaveService.class.getDeclaredField(
                        "casualLeaveEntitlement"
                );

        casualField.setAccessible(true);

        casualField.setInt(
                leaveService,
                12
        );

        Field sickField =
                LeaveService.class.getDeclaredField(
                        "sickLeaveEntitlement"
                );

        sickField.setAccessible(true);

        sickField.setInt(
                leaveService,
                12
        );

        Field annualField =
                LeaveService.class.getDeclaredField(
                        "annualLeaveEntitlement"
                );

        annualField.setAccessible(true);

        annualField.setInt(
                leaveService,
                18
        );

        /*
         * The current LeaveService.getLeaveBalance()
         * uses the derived repository query below.
         *
         * Most tests prepare data using findByEmployeeId(),
         * so this bridge returns that same data.
         */

        lenient()
                .when(
                        leaveRepository
                                .findByEmployeeIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                                        eq(1L),
                                        anyList(),
                                        any(LocalDate.class),
                                        any(LocalDate.class)
                                )
                )
                .thenAnswer(
                        invocation ->
                                leaveRepository
                                        .findByEmployeeId(1L)
                );
    }

    // ============================================================
    // CREATE
    // ============================================================

    @Test
    void createLeave_shouldCreatePendingLeaveAndCalculateDays() {

        mockAdminAuthentication();

        Employee employee =
                createEmployee(1L);

        LeaveDTO dto =
                createLeaveDTO(
                        1L,
                        "CASUAL",
                        LocalDate.of(2026, 10, 1),
                        LocalDate.of(2026, 10, 3),
                        "Family function"
                );

        when(
                employeeRepository.findById(1L)
        ).thenReturn(
                Optional.of(employee)
        );

        when(
                leaveRepository
                        .findByEmployeeIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                                eq(1L),
                                anyList(),
                                any(LocalDate.class),
                                any(LocalDate.class)
                        )
        ).thenReturn(
                List.of()
        );

        when(
                leaveRepository.save(
                        any(Leave.class)
                )
        ).thenAnswer(
                invocation -> {
                    Leave leave =
                            invocation.getArgument(0);

                    leave.setId(10L);

                    return leave;
                }
        );

        LeaveDTO result =
                leaveService.createLeave(
                        dto,
                        authentication
                );

        assertNotNull(result);

        assertEquals(
                10L,
                result.getId()
        );

        assertEquals(
                1L,
                result.getEmployeeId()
        );

        assertEquals(
                "CASUAL",
                result.getLeaveType()
        );

        assertEquals(
                "PENDING",
                result.getStatus()
        );

        assertEquals(
                3,
                result.getNumberOfDays()
        );

        verify(
                leaveRepository
        ).save(
                any(Leave.class)
        );

        verify(
                auditLogService
        ).log(
                eq("LEAVE_CREATED"),
                eq("LEAVE"),
                eq(10L),
                contains(
                        "Leave request created"
                )
        );
    }

    @Test
    void createLeave_shouldThrowWhenEmployeeDoesNotExist() {

        mockAdminAuthentication();

        LeaveDTO dto =
                createLeaveDTO(
                        999L,
                        "CASUAL",
                        LocalDate.of(2026, 10, 1),
                        LocalDate.of(2026, 10, 3),
                        "Test"
                );

        when(
                employeeRepository.findById(999L)
        ).thenReturn(
                Optional.empty()
        );

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () ->
                                leaveService.createLeave(
                                        dto,
                                        authentication
                                )
                );

        assertTrue(
                exception.getMessage()
                        .contains(
                                "Employee not found"
                        )
        );
    }

    @Test
    void createLeave_shouldRejectInvalidDateRange() {

        mockAdminAuthentication();

        Employee employee =
                createEmployee(1L);

        LeaveDTO dto =
                createLeaveDTO(
                        1L,
                        "CASUAL",
                        LocalDate.of(2026, 10, 10),
                        LocalDate.of(2026, 10, 5),
                        "Invalid dates"
                );

        when(
                employeeRepository.findById(1L)
        ).thenReturn(
                Optional.of(employee)
        );

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () ->
                                leaveService.createLeave(
                                        dto,
                                        authentication
                                )
                );

        assertEquals(
                "Leave end date cannot be before start date",
                exception.getMessage()
        );
    }

    // ============================================================
    // APPROVE
    // ============================================================
    @Test
    void approveLeave_shouldApprovePendingLeave() {

        Employee employee =
                createEmployee(1L);

        Leave leave =
                createLeaveEntity(
                        10L,
                        "PENDING"
                );

        leave.setEmployee(employee);

        when(
                leaveRepository.findById(10L)
        ).thenReturn(
                Optional.of(leave)
        );

        /*
         * No existing approved/pending leaves.
         * Therefore CASUAL balance = 12.
         * Requested = 2.
         */

        when(
                leaveRepository.save(
                        any(Leave.class)
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        LeaveDTO result =
                leaveService.approveLeave(
                        10L,
                        "Approved by manager"
                );

        assertNotNull(result);

        assertEquals(
                "APPROVED",
                leave.getStatus()
        );

        verify(
                leaveRepository
        ).findById(10L);

        verify(
                leaveRepository
        ).save(leave);
    }
    @Test
    void approveLeave_shouldRejectAlreadyProcessedLeave() {

        Leave leave =
                createLeaveEntity(
                        10L,
                        "APPROVED"
                );

        when(
                leaveRepository.findById(10L)
        ).thenReturn(
                Optional.of(leave)
        );

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () ->
                                leaveService.approveLeave(
                                        10L,
                                        "Approved"
                                )
                );

        assertEquals(
                "Only PENDING leave requests can be approved",
                exception.getMessage()
        );
    }

    // ============================================================
    // REJECT
    // ============================================================

    @Test
    void rejectLeave_shouldRejectPendingLeave() {

        Leave leave =
                createLeaveEntity(
                        10L,
                        "PENDING"
                );

        when(
                leaveRepository.findById(10L)
        ).thenReturn(
                Optional.of(leave)
        );

        when(
                leaveRepository.save(
                        any(Leave.class)
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        LeaveDTO result =
                leaveService.rejectLeave(
                        10L,
                        "Not approved"
                );

        assertEquals(
                "REJECTED",
                result.getStatus()
        );

        assertEquals(
                "Not approved",
                result.getReviewComment()
        );

        assertNotNull(
                result.getReviewedAt()
        );

        verify(
                leaveRepository
        ).save(
                leave
        );

        verify(
                auditLogService
        ).log(
                eq("LEAVE_REJECTED"),
                eq("LEAVE"),
                eq(10L),
                contains(
                        "Leave request rejected"
                )
        );
    }

    // ============================================================
    // CANCEL
    // ============================================================

    @Test
    void cancelLeave_shouldCancelPendingLeave() {

        Leave leave =
                createLeaveEntity(
                        10L,
                        "PENDING"
                );

        when(
                leaveRepository.findById(10L)
        ).thenReturn(
                Optional.of(leave)
        );

        when(
                leaveRepository.save(
                        any(Leave.class)
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        LeaveDTO result =
                leaveService.cancelLeave(10L, authentication);

        assertEquals(
                "CANCELLED",
                result.getStatus()
        );

        assertNotNull(
                result.getReviewedAt()
        );

        verify(
                leaveRepository
        ).save(
                leave
        );

        verify(
                auditLogService
        ).log(
                eq("LEAVE_CANCELLED"),
                eq("LEAVE"),
                eq(10L),
                contains(
                        "Leave request cancelled"
                )
        );
    }

    @Test
    void cancelLeave_shouldRejectNonPendingLeave() {

        Leave leave =
                createLeaveEntity(
                        10L,
                        "REJECTED"
                );

        when(
                leaveRepository.findById(10L)
        ).thenReturn(
                Optional.of(leave)
        );

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () ->
                                leaveService.cancelLeave(10L, authentication)
                );

        assertEquals(
                "Only PENDING or APPROVED leave requests can be cancelled",
                exception.getMessage()
        );
    }

    // ============================================================
    // STATUS
    // ============================================================

    @Test
    void getLeavesByStatus_shouldUseUppercaseStatus() {

        Leave leave =
                createLeaveEntity(
                        10L,
                        "PENDING"
                );

        when(
                leaveRepository
                        .findByStatusOrderByAppliedAtDesc(
                                "PENDING"
                        )
        ).thenReturn(
                List.of(leave)
        );

        List<LeaveDTO> result =
                leaveService.getLeavesByStatus(
                        "pending"
                );

        assertEquals(
                1,
                result.size()
        );

        assertEquals(
                "PENDING",
                result.get(0).getStatus()
        );

        verify(
                leaveRepository
        ).findByStatusOrderByAppliedAtDesc(
                "PENDING"
        );
    }

    // ============================================================
    // COUNT
    // ============================================================

    @Test
    void countLeavesByStatus_shouldReturnCount() {

        when(
                leaveRepository.countByStatus(
                        "PENDING"
                )
        ).thenReturn(
                2L
        );

        long result =
                leaveService.countLeavesByStatus(
                        "pending"
                );

        assertEquals(
                2L,
                result
        );

        verify(
                leaveRepository
        ).countByStatus(
                "PENDING"
        );
    }

    // ============================================================
    // UPDATE
    // ============================================================

    @Test
    void updateLeave_shouldUpdatePendingLeave() {

        mockAdminAuthentication();

        Employee employee =
                createEmployee(1L);

        Leave leave =
                createLeaveEntity(
                        10L,
                        "PENDING"
                );

        leave.setEmployee(employee);

        LeaveDTO dto =
                createLeaveDTO(
                        1L,
                        "SICK",
                        LocalDate.of(2026, 10, 10),
                        LocalDate.of(2026, 10, 12),
                        "Updated reason"
                );

        when(
                leaveRepository.findById(10L)
        ).thenReturn(
                Optional.of(leave)
        );

        when(
                leaveRepository
                        .findByEmployeeIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                                eq(1L),
                                anyList(),
                                any(LocalDate.class),
                                any(LocalDate.class)
                        )
        ).thenReturn(
                List.of()
        );

        when(
                leaveRepository.save(
                        any(Leave.class)
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        LeaveDTO result =
                leaveService.updateLeave(
                        10L,
                        dto,
                        authentication
                );

        assertEquals(
                "SICK",
                result.getLeaveType()
        );

        assertEquals(
                LocalDate.of(2026, 10, 10),
                result.getStartDate()
        );

        assertEquals(
                LocalDate.of(2026, 10, 12),
                result.getEndDate()
        );

        assertEquals(
                3,
                result.getNumberOfDays()
        );

        assertEquals(
                "Updated reason",
                result.getReason()
        );

        verify(
                leaveRepository
        ).save(
                leave
        );

        verify(
                auditLogService
        ).log(
                eq("LEAVE_UPDATED"),
                eq("LEAVE"),
                eq(10L),
                contains(
                        "Leave request updated"
                )
        );
    }

    @Test
    void updateLeave_shouldRejectNonPendingLeave() {

        mockAdminAuthentication();

        Employee employee =
                createEmployee(1L);

        Leave leave =
                createLeaveEntity(
                        10L,
                        "APPROVED"
                );

        leave.setEmployee(employee);

        LeaveDTO dto =
                createLeaveDTO(
                        1L,
                        "SICK",
                        LocalDate.of(2026, 10, 10),
                        LocalDate.of(2026, 10, 12),
                        "Updated"
                );

        when(
                leaveRepository.findById(10L)
        ).thenReturn(
                Optional.of(leave)
        );

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () ->
                                leaveService.updateLeave(
                                        10L,
                                        dto,
                                        authentication
                                )
                );

        assertEquals(
                "Only PENDING leave requests can be updated",
                exception.getMessage()
        );
    }

    // ============================================================
    // DELETE
    // ============================================================

    @Test
    void deleteLeave_shouldDeleteLeaveAndCreateAuditLog() {

        mockAdminAuthentication();

        Employee employee =
                createEmployee(1L);

        Leave leave =
                createLeaveEntity(
                        10L,
                        "PENDING"
                );

        leave.setEmployee(employee);

        when(
                leaveRepository.findById(10L)
        ).thenReturn(
                Optional.of(leave)
        );

        doNothing()
                .when(
                        leaveRepository
                )
                .delete(leave);

        leaveService.deleteLeave(
                10L,
                authentication
        );

        verify(
                leaveRepository
        ).delete(
                leave
        );

        verify(
                auditLogService
        ).log(
                eq("LEAVE_DELETED"),
                eq("LEAVE"),
                eq(10L),
                contains(
                        "Leave request deleted"
                )
        );
    }

    @Test
    void deleteLeave_shouldThrowWhenLeaveDoesNotExist() {

        mockAdminAuthentication();

        when(
                leaveRepository.findById(999L)
        ).thenReturn(
                Optional.empty()
        );

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () ->
                                leaveService.deleteLeave(
                                        999L,
                                        authentication
                                )
                );

        assertTrue(
                exception.getMessage()
                        .contains(
                                "Leave not found"
                        )
        );

        verify(
                leaveRepository,
                never()
        ).delete(
                any(Leave.class)
        );
    }

    // ============================================================
    // BALANCE - APPROVED
    // ============================================================

    @Test
    void getLeaveBalance_shouldCountApprovedDaysAsUsed() {

        Employee employee =
                createEmployee(1L);

        when(
                employeeRepository.findById(1L)
        ).thenReturn(
                Optional.of(employee)
        );

        Leave approvedLeave =
                createLeaveEntity(
                        10L,
                        "APPROVED"
                );

        approvedLeave.setLeaveType(
                "CASUAL"
        );

        approvedLeave.setStartDate(
                LocalDate.of(2026, 3, 1)
        );

        approvedLeave.setEndDate(
                LocalDate.of(2026, 3, 12)
        );

        approvedLeave.setNumberOfDays(
                12
        );

        when(
                leaveRepository.findByEmployeeId(1L)
        ).thenReturn(
                List.of(approvedLeave)
        );

        LeaveBalanceDTO result =
                leaveService.getLeaveBalance(
                        1L,
                        2026,
                        "CASUAL"
                , authentication);

        assertEquals(
                12,
                result.getAnnualEntitlement()
        );

        assertEquals(
                12,
                result.getApprovedDays()
        );

        assertEquals(
                0,
                result.getPendingDays()
        );

        assertEquals(
                0,
                result.getRemainingDays()
        );
    }

    // ============================================================
    // BALANCE - PENDING
    // ============================================================

    @Test
    void getLeaveBalance_shouldCountPendingDaysSeparately() {

        Employee employee =
                createEmployee(1L);

        when(
                employeeRepository.findById(1L)
        ).thenReturn(
                Optional.of(employee)
        );

        Leave pendingLeave =
                createLeaveEntity(
                        10L,
                        "PENDING"
                );

        pendingLeave.setLeaveType(
                "CASUAL"
        );

        pendingLeave.setStartDate(
                LocalDate.of(2026, 4, 1)
        );

        pendingLeave.setEndDate(
                LocalDate.of(2026, 4, 12)
        );

        pendingLeave.setNumberOfDays(
                12
        );

        when(
                leaveRepository.findByEmployeeId(1L)
        ).thenReturn(
                List.of(pendingLeave)
        );

        LeaveBalanceDTO result =
                leaveService.getLeaveBalance(
                        1L,
                        2026,
                        "CASUAL"
                , authentication);

        assertEquals(
                12,
                result.getAnnualEntitlement()
        );

        assertEquals(
                0,
                result.getApprovedDays()
        );

        assertEquals(
                12,
                result.getPendingDays()
        );

        assertEquals(
                12,
                result.getRemainingDays()
        );
    }

    // ============================================================
    // BALANCE - REJECTED / CANCELLED
    // ============================================================

    @Test
    void getLeaveBalance_shouldIgnoreRejectedAndCancelledLeaves() {

        Employee employee =
                createEmployee(1L);

        when(
                employeeRepository.findById(1L)
        ).thenReturn(
                Optional.of(employee)
        );

        Leave rejected =
                createLeaveEntity(
                        10L,
                        "REJECTED"
                );

        rejected.setLeaveType(
                "CASUAL"
        );

        rejected.setStartDate(
                LocalDate.of(2026, 3, 1)
        );

        rejected.setEndDate(
                LocalDate.of(2026, 3, 12)
        );

        Leave cancelled =
                createLeaveEntity(
                        11L,
                        "CANCELLED"
                );

        cancelled.setLeaveType(
                "CASUAL"
        );

        cancelled.setStartDate(
                LocalDate.of(2026, 5, 1)
        );

        cancelled.setEndDate(
                LocalDate.of(2026, 5, 12)
        );

        when(
                leaveRepository.findByEmployeeId(1L)
        ).thenReturn(
                List.of(
                        rejected,
                        cancelled
                )
        );

        LeaveBalanceDTO result =
                leaveService.getLeaveBalance(
                        1L,
                        2026,
                        "CASUAL"
                , authentication);

        assertEquals(
                0,
                result.getApprovedDays()
        );

        assertEquals(
                0,
                result.getPendingDays()
        );

        assertEquals(
                12,
                result.getRemainingDays()
        );
    }

    // ============================================================
    // BALANCE - OTHER TYPES
    // ============================================================

    @Test
    void getLeaveBalance_shouldIgnoreOtherLeaveTypes() {

        Employee employee =
                createEmployee(1L);

        when(
                employeeRepository.findById(1L)
        ).thenReturn(
                Optional.of(employee)
        );

        Leave sickLeave =
                createLeaveEntity(
                        10L,
                        "APPROVED"
                );

        sickLeave.setLeaveType(
                "SICK"
        );

        sickLeave.setStartDate(
                LocalDate.of(2026, 3, 1)
        );

        sickLeave.setEndDate(
                LocalDate.of(2026, 3, 12)
        );

        when(
                leaveRepository.findByEmployeeId(1L)
        ).thenReturn(
                List.of(sickLeave)
        );

        LeaveBalanceDTO result =
                leaveService.getLeaveBalance(
                        1L,
                        2026,
                        "CASUAL"
                , authentication);

        assertEquals(
                0,
                result.getApprovedDays()
        );

        assertEquals(
                12,
                result.getRemainingDays()
        );
    }

    // ============================================================
    // BALANCE - YEAR CALCULATION
    // ============================================================

    @Test
    void getLeaveBalance_shouldCalculateOnlyDaysInsideRequestedYear() {

        Employee employee =
                createEmployee(1L);

        when(
                employeeRepository.findById(1L)
        ).thenReturn(
                Optional.of(employee)
        );

        Leave leave =
                createLeaveEntity(
                        10L,
                        "APPROVED"
                );

        leave.setLeaveType(
                "CASUAL"
        );

        /*
         * Dec 28 2025 -> Jan 6 2026
         *
         * Days inside 2026:
         * Jan 1 -> Jan 6 = 6 days
         */

        leave.setStartDate(
                LocalDate.of(2025, 12, 28)
        );

        leave.setEndDate(
                LocalDate.of(2026, 1, 6)
        );

        when(
                leaveRepository.findByEmployeeId(1L)
        ).thenReturn(
                List.of(leave)
        );

        LeaveBalanceDTO result =
                leaveService.getLeaveBalance(
                        1L,
                        2026,
                        "CASUAL"
                , authentication);

        assertEquals(
                6,
                result.getApprovedDays()
        );

        assertEquals(
                6,
                result.getApprovedDays()
        );

        assertEquals(
                6,
                result.getApprovedDays()
        );
    }

    // ============================================================
    // BALANCE - SICK
    // ============================================================

    @Test
    void getLeaveBalance_shouldReturnSickEntitlement() {

        Employee employee =
                createEmployee(1L);

        when(
                employeeRepository.findById(1L)
        ).thenReturn(
                Optional.of(employee)
        );

        when(
                leaveRepository.findByEmployeeId(1L)
        ).thenReturn(
                List.of()
        );

        LeaveBalanceDTO result =
                leaveService.getLeaveBalance(
                        1L,
                        2026,
                        "SICK"
                , authentication);

        assertEquals(
                12,
                result.getAnnualEntitlement()
        );

        assertEquals(
                "SICK",
                result.getLeaveType()
        );

        assertEquals(
                12,
                result.getRemainingDays()
        );
    }

    // ============================================================
    // BALANCE - ANNUAL
    // ============================================================

    @Test
    void getLeaveBalance_shouldReturnAnnualEntitlement() {

        Employee employee =
                createEmployee(1L);

        when(
                employeeRepository.findById(1L)
        ).thenReturn(
                Optional.of(employee)
        );

        when(
                leaveRepository.findByEmployeeId(1L)
        ).thenReturn(
                List.of()
        );

        LeaveBalanceDTO result =
                leaveService.getLeaveBalance(
                        1L,
                        2026,
                        "ANNUAL"
                , authentication);

        assertEquals(
                18,
                result.getAnnualEntitlement()
        );

        assertEquals(
                "ANNUAL",
                result.getLeaveType()
        );

        assertEquals(
                18,
                result.getRemainingDays()
        );
    }

    // ============================================================
    // BALANCE - INVALID TYPE
    // ============================================================

    @Test
    void getLeaveBalance_shouldThrowForUnknownLeaveType() {

        Employee employee =
                createEmployee(1L);

        when(
                employeeRepository.findById(1L)
        ).thenReturn(
                Optional.of(employee)
        );

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () ->
                                leaveService.getLeaveBalance(
                                        1L,
                                        2026,
                                        "BEREAVEMENT"
                                , authentication)
                );

        assertTrue(
                exception.getMessage()
                        .toLowerCase()
                        .contains(
                                "leave type"
                        )
        );
    }

    // ============================================================
    // BALANCE - EMPLOYEE NOT FOUND
    // ============================================================

    @Test
    void getLeaveBalance_shouldThrowWhenEmployeeDoesNotExist() {

        when(
                employeeRepository.findById(999L)
        ).thenReturn(
                Optional.empty()
        );

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () ->
                                leaveService.getLeaveBalance(
                                        999L,
                                        2026,
                                        "CASUAL",
                                        authentication
                                )
                );

        assertTrue(
                exception.getMessage()
                        .contains(
                                "Employee not found"
                        )
        );
    }

    // ============================================================
    // BALANCE - ALL TYPES
    // ============================================================

    @Test
    void getEmployeeLeaveBalances_shouldReturnAllSupportedTypes() {

        Employee employee =
                createEmployee(1L);

        when(
                employeeRepository.findById(1L)
        ).thenReturn(
                Optional.of(employee)
        );

        when(
                leaveRepository.findByEmployeeId(1L)
        ).thenReturn(
                List.of()
        );

        List<LeaveBalanceDTO> result =
                leaveService.getEmployeeLeaveBalances(
                        1L,
                        2026
                , authentication);

        assertEquals(
                7,
                result.size()
        );

        assertEquals(
                "CASUAL",
                result.get(0).getLeaveType()
        );

        assertEquals(
                "SICK",
                result.get(1).getLeaveType()
        );

        assertEquals(
                "ANNUAL",
                result.get(2).getLeaveType()
        );

        assertEquals(
                "MATERNITY",
                result.get(3).getLeaveType()
        );

        assertEquals(
                "PATERNITY",
                result.get(4).getLeaveType()
        );

        assertEquals(
                "UNPAID",
                result.get(5).getLeaveType()
        );

        assertEquals(
                "OTHER",
                result.get(6).getLeaveType()
        );

        assertEquals(
                12,
                result.get(0).getAnnualEntitlement()
        );

        assertEquals(
                12,
                result.get(1).getAnnualEntitlement()
        );

        assertEquals(
                18,
                result.get(2).getAnnualEntitlement()
        );
    }

    // ============================================================
    // BALANCE - EXCEEDS ENTITLEMENT
    // ============================================================

    @Test
    void getLeaveBalance_shouldAllowApprovedDaysToExceedEntitlement() {

        Employee employee =
                createEmployee(1L);

        when(
                employeeRepository.findById(1L)
        ).thenReturn(
                Optional.of(employee)
        );

        Leave leave =
                createLeaveEntity(
                        10L,
                        "APPROVED"
                );

        leave.setLeaveType(
                "CASUAL"
        );

        leave.setStartDate(
                LocalDate.of(2026, 1, 1)
        );

        leave.setEndDate(
                LocalDate.of(2026, 1, 15)
        );

        leave.setNumberOfDays(
                15
        );

        when(
                leaveRepository.findByEmployeeId(1L)
        ).thenReturn(
                List.of(leave)
        );

        LeaveBalanceDTO result =
                leaveService.getLeaveBalance(
                        1L,
                        2026,
                        "CASUAL"
                , authentication);

        assertEquals(
                15,
                result.getApprovedDays()
        );

        /*
         * LeaveService uses Math.max(0, entitlement - approvedDays),
         * so remainingDays cannot become negative.
         */
        assertEquals(
                0,
                result.getRemainingDays()
        );
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private void mockAdminAuthentication() {

        lenient()
                .doReturn(true)
                .when(authentication)
                .isAuthenticated();

        lenient()
                .doReturn("admin")
                .when(authentication)
                .getName();

        lenient()
                .doReturn(
                        List.of(
                                new SimpleGrantedAuthority(
                                        "ROLE_ADMIN"
                                )
                        )
                )
                .when(authentication)
                .getAuthorities();
    }

    private Employee createEmployee(
            Long id
    ) {

        Employee employee =
                new Employee();

        employee.setId(id);

        return employee;
    }

    private Leave createLeaveEntity(
            Long id,
            String status
    ) {

        Employee employee =
                createEmployee(1L);

        Leave leave =
                new Leave();

        leave.setId(id);

        leave.setEmployee(
                employee
        );

        leave.setLeaveType(
                "CASUAL"
        );

        leave.setStartDate(
                LocalDate.of(
                        2026,
                        1,
                        1
                )
        );

        leave.setEndDate(
                LocalDate.of(
                        2026,
                        1,
                        2
                )
        );

        leave.setNumberOfDays(
                2
        );

        leave.setReason(
                "Test leave"
        );

        leave.setStatus(
                status
        );

        leave.setAppliedAt(
                LocalDateTime.now()
        );

        return leave;
    }

    private LeaveDTO createLeaveDTO(
            Long employeeId,
            String leaveType,
            LocalDate startDate,
            LocalDate endDate,
            String reason
    ) {

        LeaveDTO dto =
                new LeaveDTO();

        dto.setEmployeeId(
                employeeId
        );

        dto.setLeaveType(
                leaveType
        );

        dto.setStartDate(
                startDate
        );

        dto.setEndDate(
                endDate
        );

        dto.setReason(
                reason
        );

        return dto;
    }
}




