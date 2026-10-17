package com.nexushr.service;

import com.nexushr.dto.LeaveBalanceDTO;
import com.nexushr.dto.LeaveDTO;
import com.nexushr.entity.Employee;
import com.nexushr.entity.Leave;
import com.nexushr.exception.AuthorizationException;
import com.nexushr.repository.EmployeeRepository;
import com.nexushr.repository.LeaveRepository;
import com.nexushr.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class LeaveService {

    private final LeaveRepository leaveRepository;
    private final EmployeeRepository employeeRepository;
    private final AuditLogService auditLogService;

    @Autowired(required = false)
    private NotificationService notificationService;

    @Autowired(required = false)
    private EmailNotificationService emailNotificationService;

    @Autowired(required = false)
    private UserRepository userRepository;

    @Value("${leave.entitlement.casual:12}")
    private int casualLeaveEntitlement;

    @Value("${leave.entitlement.sick:12}")
    private int sickLeaveEntitlement;

    @Value("${leave.entitlement.annual:18}")
    private int annualLeaveEntitlement;

    @Value("${leave.entitlement.maternity:182}")
    private int maternityLeaveEntitlement;

    @Value("${leave.entitlement.paternity:15}")
    private int paternityLeaveEntitlement;

    @Value("${leave.entitlement.unpaid:365}")
    private int unpaidLeaveEntitlement;

    @Value("${leave.entitlement.other:30}")
    private int otherLeaveEntitlement;

    public LeaveService(
            LeaveRepository leaveRepository,
            EmployeeRepository employeeRepository,
            AuditLogService auditLogService
    ) {
        this.leaveRepository = leaveRepository;
        this.employeeRepository = employeeRepository;
        this.auditLogService = auditLogService;
    }

    // ============================================================
    // CREATE
    // ============================================================

    public LeaveDTO createLeave(
            LeaveDTO dto,
            Authentication authentication
    ) {

        if (authentication == null ||
                !authentication.isAuthenticated()) {

            throw new AuthorizationException(
                    "Authentication is required"
            );
        }

        Employee employee =
                employeeRepository.findById(
                        dto.getEmployeeId()
                ).orElseThrow(() ->
                        new RuntimeException(
                                "Employee not found with ID: "
                                        + dto.getEmployeeId()
                        )
                );

        validateLeaveCreationOwnership(
                employee,
                authentication
        );

        validateLeaveDates(
                dto.getStartDate(),
                dto.getEndDate()
        );

        validateLeaveType(
                dto.getLeaveType()
        );

        validateNoOverlap(
                employee.getId(),
                dto.getStartDate(),
                dto.getEndDate(),
                null
        );

        int numberOfDays =
                calculateNumberOfDays(
                        dto.getStartDate(),
                        dto.getEndDate()
                );

        Leave leave = new Leave();

        leave.setEmployee(employee);

        leave.setLeaveType(
                normalizeLeaveType(
                        dto.getLeaveType()
                )
        );

        leave.setStartDate(
                dto.getStartDate()
        );

        leave.setEndDate(
                dto.getEndDate()
        );

        leave.setNumberOfDays(
                numberOfDays
        );

        leave.setReason(
                dto.getReason()
        );

        leave.setStatus(
                "PENDING"
        );

        leave.setAppliedAt(
                LocalDateTime.now()
        );

        Leave savedLeave =
                leaveRepository.save(leave);

        auditLogService.log(
                "LEAVE_CREATED",
                "LEAVE",
                savedLeave.getId(),
                "Leave request created for employee ID: "
                        + employee.getId()
        );

        sendLeaveApprovalRequestEmails(savedLeave);

        return convertToDTO(savedLeave);
    }

    // ============================================================
    // READ ALL
    // ============================================================

    @Transactional(readOnly = true)
    public List<LeaveDTO> getAllLeaves() {

        return leaveRepository
                .findAllByOrderByAppliedAtDesc()
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    // ============================================================
    // READ BY ID
    // ============================================================

    @Transactional(readOnly = true)
    public LeaveDTO getLeaveById(Long id) {

        Leave leave =
                leaveRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Leave not found with ID: "
                                                + id
                                )
                        );

        return convertToDTO(leave);
    }

    // ============================================================
    // READ EMPLOYEE LEAVES
    // ============================================================

    @Transactional(readOnly = true)
    public List<LeaveDTO> getEmployeeLeaves(
            Long employeeId,
            Authentication authentication
    ) {

        Employee employee =
                employeeRepository.findById(employeeId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Employee not found with ID: "
                                                + employeeId
                                )
                        );

        validateEmployeeOwnership(
                employee,
                authentication
        );

        return leaveRepository
                .findByEmployeeId(employeeId)
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    // ============================================================
    // BALANCE - ALL TYPES
    // ============================================================

    @Transactional(readOnly = true)
    public List<LeaveBalanceDTO> getEmployeeLeaveBalances(
            Long employeeId,
            int year,
            Authentication authentication
    ) {

        Employee employee =
                employeeRepository.findById(employeeId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Employee not found with ID: "
                                                + employeeId
                                )
                        );

        validateEmployeeOwnership(
                employee,
                authentication
        );

        return List.of(
                getLeaveBalanceInternal(employeeId, year, "CASUAL"),
                getLeaveBalanceInternal(employeeId, year, "SICK"),
                getLeaveBalanceInternal(employeeId, year, "ANNUAL"),
                getLeaveBalanceInternal(employeeId, year, "MATERNITY"),
                getLeaveBalanceInternal(employeeId, year, "PATERNITY"),
                getLeaveBalanceInternal(employeeId, year, "UNPAID"),
                getLeaveBalanceInternal(employeeId, year, "OTHER")
        );
    }

    // ============================================================
    // BALANCE - SPECIFIC TYPE
    // ============================================================

    @Transactional(readOnly = true)
    public LeaveBalanceDTO getLeaveBalance(
            Long employeeId,
            int year,
            String leaveType,
            Authentication authentication
    ) {

        Employee employee =
                employeeRepository.findById(employeeId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Employee not found with ID: "
                                                + employeeId
                                )
                        );

        validateEmployeeOwnership(
                employee,
                authentication
        );

        return getLeaveBalanceInternal(
                employeeId,
                year,
                leaveType
        );
    }

    // ============================================================
    // BALANCE INTERNAL
    // ============================================================

    private LeaveBalanceDTO getLeaveBalanceInternal(
            Long employeeId,
            int year,
            String leaveType
    ) {

        if (year < 1900 || year > 3000) {

            throw new RuntimeException(
                    "Invalid leave balance year"
            );
        }

        validateLeaveType(
                leaveType
        );

        String normalizedType =
                normalizeLeaveType(
                        leaveType
                );

        int entitlement =
                getEntitlement(
                        normalizedType
                );

        LocalDate yearStart =
                LocalDate.of(
                        year,
                        1,
                        1
                );

        LocalDate yearEnd =
                LocalDate.of(
                        year,
                        12,
                        31
                );

        List<String> statuses =
                Arrays.asList(
                        "APPROVED",
                        "PENDING"
                );

        List<Leave> leaves =
                leaveRepository
                        .findByEmployeeIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                                employeeId,
                                statuses,
                                yearEnd,
                                yearStart
                        );

        int approvedDays = 0;
        int pendingDays = 0;

        for (Leave leave : leaves) {

            if (!normalizedType.equalsIgnoreCase(
                    leave.getLeaveType()
            )) {
                continue;
            }

            int days =
                    calculateDaysWithinYear(
                            leave.getStartDate(),
                            leave.getEndDate(),
                            year
                    );

            if ("APPROVED".equalsIgnoreCase(
                    leave.getStatus()
            )) {

                approvedDays += days;

            } else if ("PENDING".equalsIgnoreCase(
                    leave.getStatus()
            )) {

                pendingDays += days;
            }
        }

        int remainingDays =
                Math.max(
                        0,
                        entitlement - approvedDays
                );

        return new LeaveBalanceDTO(
                employeeId,
                year,
                normalizedType,
                entitlement,
                approvedDays,
                pendingDays,
                remainingDays
        );
    }

    // ============================================================
    // FILTER BY STATUS
    // ============================================================

    @Transactional(readOnly = true)
    public List<LeaveDTO> getLeavesByStatus(
            String status
    ) {

        return leaveRepository
                .findByStatusOrderByAppliedAtDesc(
                        status.toUpperCase()
                )
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    // ============================================================
    // FILTER BY TYPE
    // ============================================================

    @Transactional(readOnly = true)
    public List<LeaveDTO> getLeavesByType(
            String leaveType
    ) {

        validateLeaveType(
                leaveType
        );

        return leaveRepository
                .findByLeaveType(
                        normalizeLeaveType(
                                leaveType
                        )
                )
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    // ============================================================
    // APPROVE
    // ============================================================

    public LeaveDTO approveLeave(
            Long id,
            String reviewComment
    ) {
        return approveLeave(id, reviewComment, null);
    }

    public LeaveDTO approveLeave(
            Long id,
            String reviewComment,
            Authentication authentication
    ) {

        Leave leave =
                findLeave(id);

        validateManagerLeaveDecisionAccess(leave, authentication);

        if (!"PENDING".equalsIgnoreCase(
                leave.getStatus()
        )) {

            throw new RuntimeException(
                    "Only PENDING leave requests can be approved"
            );
        }

        validateNoOverlap(
                leave.getEmployee().getId(),
                leave.getStartDate(),
                leave.getEndDate(),
                leave.getId()
        );

        String leaveType =
                normalizeLeaveType(
                        leave.getLeaveType()
                );

        int year =
                leave.getStartDate()
                        .getYear();

        LeaveBalanceDTO balance =
                getLeaveBalanceInternal(
                        leave.getEmployee().getId(),
                        year,
                        leaveType
                );

        int requestedDays =
                calculateDaysWithinYear(
                        leave.getStartDate(),
                        leave.getEndDate(),
                        year
                );

        if (requestedDays >
                balance.getRemainingDays()) {

            throw new RuntimeException(
                    "Insufficient "
                            + leaveType
                            + " leave balance. Remaining: "
                            + balance.getRemainingDays()
                            + ", Requested: "
                            + requestedDays
            );
        }

        leave.setStatus(
                "APPROVED"
        );

        leave.setReviewedAt(
                LocalDateTime.now()
        );

        if (reviewComment != null) {

            leave.setReviewComment(
                    reviewComment.trim()
            );
        }

        Leave savedLeave =
                leaveRepository.save(
                        leave
                );

        auditLogService.log(
                "LEAVE_APPROVED",
                "LEAVE",
                savedLeave.getId(),
                "Leave request approved for employee ID: "
                        + leave.getEmployee().getId()
        );

        sendLeaveDecisionNotification(savedLeave, "APPROVED", reviewComment, authentication);

        return convertToDTO(
                savedLeave
        );
    }

    // ============================================================
    // REJECT
    // ============================================================

    public LeaveDTO rejectLeave(
            Long id,
            String reviewComment
    ) {
        return rejectLeave(id, reviewComment, null);
    }

    public LeaveDTO rejectLeave(
            Long id,
            String reviewComment,
            Authentication authentication
    ) {

        Leave leave =
                findLeave(id);

        validateManagerLeaveDecisionAccess(leave, authentication);

        if (!"PENDING".equalsIgnoreCase(
                leave.getStatus()
        )) {

            throw new RuntimeException(
                    "Only PENDING leave requests can be rejected"
            );
        }

        leave.setStatus(
                "REJECTED"
        );

        leave.setReviewedAt(
                LocalDateTime.now()
        );

        if (reviewComment != null) {

            leave.setReviewComment(
                    reviewComment.trim()
            );
        }

        Leave savedLeave =
                leaveRepository.save(
                        leave
                );

        auditLogService.log(
                "LEAVE_REJECTED",
                "LEAVE",
                savedLeave.getId(),
                "Leave request rejected for employee ID: "
                        + leave.getEmployee().getId()
        );

        sendLeaveDecisionNotification(savedLeave, "REJECTED", reviewComment, authentication);

        return convertToDTO(
                savedLeave
        );
    }
    // ============================================================
    // CANCEL
    // ============================================================

    public LeaveDTO cancelLeave(
            Long id,
            Authentication authentication
    ) {

        Leave leave =
                findLeave(id);

        validateLeaveCancellationAccess(
                leave.getEmployee(),
                authentication
        );

        String status =
                leave.getStatus() == null
                        ? ""
                        : leave.getStatus().trim().toUpperCase();

        if (!"PENDING".equals(status) &&
                !"APPROVED".equals(status)) {

            throw new RuntimeException(
                    "Only PENDING or APPROVED leave requests can be cancelled"
            );
        }

        leave.setStatus(
                "CANCELLED"
        );

        leave.setReviewedAt(
                LocalDateTime.now()
        );

        Leave savedLeave =
                leaveRepository.save(
                        leave
                );

        auditLogService.log(
                "LEAVE_CANCELLED",
                "LEAVE",
                savedLeave.getId(),
                "Leave request cancelled for employee ID: "
                        + leave.getEmployee().getId()
        );

        return convertToDTO(
                savedLeave
        );
    }

    // ============================================================
    // UPDATE
    // ============================================================

    public LeaveDTO updateLeave(
            Long id,
            LeaveDTO dto,
            Authentication authentication
    ) {

        Leave leave =
                findLeave(id);

        if (!"PENDING".equalsIgnoreCase(
                leave.getStatus()
        )) {

            throw new RuntimeException(
                    "Only PENDING leave requests can be updated"
            );
        }

        validateEmployeeOwnership(
                leave.getEmployee(),
                authentication
        );

        if (dto.getEmployeeId() != null &&
                !leave.getEmployee()
                        .getId()
                        .equals(dto.getEmployeeId())) {

            throw new RuntimeException(
                    "Employee cannot be changed"
            );
        }

        validateLeaveDates(
                dto.getStartDate(),
                dto.getEndDate()
        );

        validateLeaveType(
                dto.getLeaveType()
        );

        validateNoOverlap(
                leave.getEmployee().getId(),
                dto.getStartDate(),
                dto.getEndDate(),
                leave.getId()
        );

        leave.setLeaveType(
                normalizeLeaveType(
                        dto.getLeaveType()
                )
        );

        leave.setStartDate(
                dto.getStartDate()
        );

        leave.setEndDate(
                dto.getEndDate()
        );

        leave.setNumberOfDays(
                calculateNumberOfDays(
                        dto.getStartDate(),
                        dto.getEndDate()
                )
        );

        leave.setReason(
                dto.getReason()
        );

        Leave savedLeave =
                leaveRepository.save(
                        leave
                );

        auditLogService.log(
                "LEAVE_UPDATED",
                "LEAVE",
                savedLeave.getId(),
                "Leave request updated for employee ID: "
                        + leave.getEmployee().getId()
        );

        return convertToDTO(
                savedLeave
        );
    }

    // ============================================================
    // DELETE
    // ============================================================

    public void deleteLeave(
            Long id,
            Authentication authentication
    ) {

        Leave leave =
                findLeave(id);

        validateEmployeeOwnership(
                leave.getEmployee(),
                authentication
        );

        if (!"PENDING".equalsIgnoreCase(
                leave.getStatus()
        )) {

            throw new RuntimeException(
                    "Only PENDING leave requests can be deleted"
            );
        }

        leaveRepository.delete(
                leave
        );

        auditLogService.log(
                "LEAVE_DELETED",
                "LEAVE",
                id,
                "Leave request deleted with ID: "
                        + id
        );
    }

    // ============================================================
    // DATE RANGE
    // ============================================================

    @Transactional(readOnly = true)
    public List<LeaveDTO> getLeavesBetweenDates(
            LocalDate startDate,
            LocalDate endDate
    ) {

        validateLeaveDates(
                startDate,
                endDate
        );

        return leaveRepository
                .findByStartDateBetween(
                        startDate,
                        endDate
                )
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    // ============================================================
    // EMPLOYEE DATE RANGE
    // ============================================================

    @Transactional(readOnly = true)
    public List<LeaveDTO> getEmployeeLeavesBetweenDates(
            Long employeeId,
            LocalDate startDate,
            LocalDate endDate,
            Authentication authentication
    ) {

        Employee employee =
                employeeRepository.findById(
                        employeeId
                ).orElseThrow(() ->
                        new RuntimeException(
                                "Employee not found with ID: "
                                        + employeeId
                        )
                );

        validateEmployeeOwnership(
                employee,
                authentication
        );

        validateLeaveDates(
                startDate,
                endDate
        );

        return leaveRepository
                .findByEmployeeIdAndStartDateBetween(
                        employeeId,
                        startDate,
                        endDate
                )
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    // ============================================================
    // COUNT
    // ============================================================

    @Transactional(readOnly = true)
    public long countLeavesByStatus(
            String status
    ) {

        return leaveRepository
                .countByStatus(
                        status.toUpperCase()
                );
    }

    // ============================================================
    // FIND LEAVE
    // ============================================================

    private Leave findLeave(
            Long id
    ) {

        return leaveRepository
                .findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Leave not found with ID: "
                                        + id
                        )
                );
    }



    private void validateLeaveCreationOwnership(Employee employee, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthorizationException("Authentication is required");
        }

        boolean adminOrHr = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())
                        || "ROLE_HR".equals(authority.getAuthority()));
        if (adminOrHr) {
            return;
        }

        if (employee.getUser() == null || employee.getUser().getUsername() == null
                || !authentication.getName().equalsIgnoreCase(employee.getUser().getUsername())) {
            throw new AuthorizationException("Managers and employees can only raise leave requests for themselves");
        }
    }

    /**
     * A manager may raise and view their own leave through self-service, but a
     * manager's leave request is a conflict-sensitive request and can only be
     * approved/rejected by an ADMIN. Employee leave keeps the existing approval
     * workflow for ADMIN/HR/MANAGER.
     */
    private void validateManagerLeaveDecisionAccess(Leave leave, Authentication authentication) {
        if (leave == null || leave.getEmployee() == null || leave.getEmployee().getUser() == null) {
            return;
        }

        String employeeRole = leave.getEmployee().getUser().getRole();
        boolean managerLeave = employeeRole != null
                && employeeRole.replace("ROLE_", "").equalsIgnoreCase("MANAGER");

        if (!managerLeave) {
            return;
        }

        boolean admin = authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));

        if (!admin) {
            throw new AuthorizationException(
                    "Manager leave requests can only be approved or rejected by an Admin"
            );
        }
    }

    /**
     * Admin/HR may cancel managed leave when policy requires it. Managers and
     * employees may only cancel their own request through self-service. This
     * prevents a manager from cancelling another employee's approved leave.
     */
    private void validateLeaveCancellationAccess(
            Employee employee,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthorizationException("Authentication is required");
        }

        boolean adminOrHr = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())
                        || "ROLE_HR".equals(authority.getAuthority()));

        if (adminOrHr) {
            return;
        }

        if (employee == null || employee.getUser() == null
                || employee.getUser().getUsername() == null
                || !authentication.getName().equalsIgnoreCase(employee.getUser().getUsername())) {
            throw new AuthorizationException(
                    "You can only cancel your own leave request"
            );
        }
    }

    // ============================================================
    // EMPLOYEE OWNERSHIP / AUTHORIZATION
    // ============================================================

    private void validateEmployeeOwnership(
            Employee employee,
            Authentication authentication
    ) {

        if (authentication == null ||
                !authentication.isAuthenticated()) {

            throw new AuthorizationException(
                    "Authentication is required"
            );
        }

        boolean privilegedUser =
                authentication
                        .getAuthorities()
                        .stream()
                        .anyMatch(authority -> {

                            String role =
                                    authority.getAuthority();

                            return role.equals(
                                    "ROLE_ADMIN"
                            )
                                    || role.equals(
                                    "ROLE_HR"
                            )
                                    || role.equals(
                                    "ROLE_MANAGER"
                            );
                        });

        if (privilegedUser) {
            return;
        }

        if (employee.getUser() == null) {

            throw new AuthorizationException(
                    "Employee is not linked to a user account"
            );
        }

        String authenticatedUsername =
                authentication.getName();

        String employeeUsername =
                employee.getUser()
                        .getUsername();

        if (!authenticatedUsername.equalsIgnoreCase(
                employeeUsername
        )) {

            throw new AuthorizationException(
                    "You are not authorized to manage this employee's leave"
            );
        }
    }

    // ============================================================
    // DATE VALIDATION
    // ============================================================

    private void validateLeaveDates(
            LocalDate startDate,
            LocalDate endDate
    ) {

        if (startDate == null ||
                endDate == null) {

            throw new RuntimeException(
                    "Leave start date and end date are required"
            );
        }

        if (endDate.isBefore(
                startDate
        )) {

            throw new RuntimeException(
                    "Leave end date cannot be before start date"
            );
        }
    }

    // ============================================================
    // LEAVE TYPE VALIDATION
    // ============================================================

    private void validateLeaveType(
            String leaveType
    ) {

        if (leaveType == null ||
                leaveType.trim().isEmpty()) {

            throw new RuntimeException(
                    "Leave type is required"
            );
        }

        String normalized =
                normalizeLeaveType(
                        leaveType
                );

        if (!Arrays.asList(
                "CASUAL",
                "SICK",
                "ANNUAL",
                "MATERNITY",
                "PATERNITY",
                "UNPAID",
                "OTHER"
        ).contains(normalized)) {

            throw new RuntimeException(
                    "Invalid leave type. Allowed values: CASUAL, SICK, ANNUAL, MATERNITY, PATERNITY, UNPAID, OTHER"
            );
        }
    }

    // ============================================================
    // NORMALIZE LEAVE TYPE
    // ============================================================

    private String normalizeLeaveType(
            String leaveType
    ) {

        return leaveType
                .trim()
                .toUpperCase(
                        Locale.ROOT
                );
    }

    // ============================================================
    // CALCULATE DAYS
    // ============================================================

    private int calculateNumberOfDays(
            LocalDate startDate,
            LocalDate endDate
    ) {

        return (int)
                (
                        endDate.toEpochDay()
                                - startDate.toEpochDay()
                )
                + 1;
    }

    // ============================================================
    // CALCULATE DAYS WITHIN YEAR
    // ============================================================

    private int calculateDaysWithinYear(
            LocalDate startDate,
            LocalDate endDate,
            int year
    ) {

        LocalDate yearStart =
                LocalDate.of(
                        year,
                        1,
                        1
                );

        LocalDate yearEnd =
                LocalDate.of(
                        year,
                        12,
                        31
                );

        LocalDate effectiveStart =
                startDate.isBefore(
                        yearStart
                )
                        ? yearStart
                        : startDate;

        LocalDate effectiveEnd =
                endDate.isAfter(
                        yearEnd
                )
                        ? yearEnd
                        : endDate;

        if (effectiveEnd.isBefore(
                effectiveStart
        )) {

            return 0;
        }

        return calculateNumberOfDays(
                effectiveStart,
                effectiveEnd
        );
    }

    // ============================================================
    // OVERLAP VALIDATION
    // ============================================================

    private void validateNoOverlap(
            Long employeeId,
            LocalDate startDate,
            LocalDate endDate,
            Long excludeLeaveId
    ) {

        List<String> statuses =
                Arrays.asList(
                        "PENDING",
                        "APPROVED"
                );

        List<Leave> overlappingLeaves =
                leaveRepository
                        .findByEmployeeIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                                employeeId,
                                statuses,
                                endDate,
                                startDate
                        );

        for (Leave existingLeave :
                overlappingLeaves) {

            if (excludeLeaveId != null &&
                    excludeLeaveId.equals(
                            existingLeave.getId()
                    )) {

                continue;
            }

            throw new RuntimeException(
                    "Leave dates overlap with an existing "
                            + existingLeave
                            .getStatus()
                            .toLowerCase()
                            + " leave request"
            );
        }
    }

    // ============================================================
    // ENTITLEMENT
    // ============================================================

    private int getEntitlement(
            String leaveType
    ) {

        return switch (
                normalizeLeaveType(
                        leaveType
                )
                ) {

            case "CASUAL" ->
                    casualLeaveEntitlement;

            case "SICK" ->
                    sickLeaveEntitlement;

            case "ANNUAL" ->
                    annualLeaveEntitlement;

            case "MATERNITY" ->
                    maternityLeaveEntitlement;

            case "PATERNITY" ->
                    paternityLeaveEntitlement;

            case "UNPAID" ->
                    unpaidLeaveEntitlement;

            case "OTHER" ->
                    otherLeaveEntitlement;

            default ->
                    throw new RuntimeException(
                            "Unsupported leave type: "
                                    + leaveType
                    );
        };
    }

    // ============================================================
    // CONVERT ENTITY TO DTO
    // ============================================================

    private LeaveDTO convertToDTO(
            Leave leave
    ) {

        LeaveDTO dto =
                new LeaveDTO();

        dto.setId(
                leave.getId()
        );

        dto.setEmployeeId(
                leave.getEmployee()
                        .getId()
        );

        dto.setLeaveType(
                leave.getLeaveType()
        );

        dto.setStartDate(
                leave.getStartDate()
        );

        dto.setEndDate(
                leave.getEndDate()
        );

        dto.setNumberOfDays(
                leave.getNumberOfDays()
        );

        dto.setReason(
                leave.getReason()
        );

        dto.setStatus(
                leave.getStatus()
        );

        dto.setAppliedAt(
                leave.getAppliedAt()
        );

        dto.setReviewedAt(
                leave.getReviewedAt()
        );

        dto.setReviewComment(
                leave.getReviewComment()
        );

        return dto;
    }
    private void sendLeaveApprovalRequestEmails(Leave leave) {
        if (leave == null || leave.getEmployee() == null
                || emailNotificationService == null || userRepository == null) {
            return;
        }

        Employee employee = leave.getEmployee();
        String employeeName = ((employee.getFirstName() == null ? "" : employee.getFirstName())
                + " "
                + (employee.getLastName() == null ? "" : employee.getLastName())).trim();

        String subject = "Leave Approval Required - " + employeeName;
        String message = "A new leave request requires review.\n\n"
                + "Employee: " + employeeName + " (#" + employee.getId() + ")\n"
                + "Leave Type: " + leave.getLeaveType() + "\n"
                + "Dates: " + leave.getStartDate() + " to " + leave.getEndDate() + "\n"
                + "Days: " + leave.getNumberOfDays() + "\n"
                + "Reason: " + leave.getReason() + "\n\n"
                + "Open NexusHR > Leave Management to approve or reject the request.";

        userRepository.findAll().stream()
                .filter(user -> user.isEnabled())
                .filter(user -> {
                    String role = user.getRole() == null ? "" : user.getRole().toUpperCase(Locale.ROOT);
                    return role.equals("ADMIN") || role.equals("HR") || role.equals("MANAGER");
                })
                .filter(user -> user.getRecoveryEmail() != null && !user.getRecoveryEmail().isBlank())
                .map(user -> user.getRecoveryEmail().trim().toLowerCase(Locale.ROOT))
                .distinct()
                .forEach(email -> emailNotificationService.sendToAddress(email, subject, message));
    }

    private void sendLeaveDecisionNotification(
            Leave leave,
            String decision,
            String reviewComment,
            Authentication authentication
    ) {
        if (leave == null || leave.getEmployee() == null) return;

        Employee employee = leave.getEmployee();
        String sender = authentication == null || authentication.getName() == null
                ? "SYSTEM"
                : authentication.getName();
        String role = authentication == null || authentication.getAuthorities() == null
                ? "SYSTEM"
                : authentication.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(value -> value != null && value.startsWith("ROLE_"))
                .map(value -> value.substring(5))
                .findFirst()
                .orElse("SYSTEM");

        String subject = "Leave " + ("APPROVED".equals(decision) ? "Approved" : "Rejected")
                + " - " + leave.getStartDate() + " to " + leave.getEndDate();
        StringBuilder message = new StringBuilder();
        message.append("Your ").append(leave.getLeaveType())
                .append(" leave request from ").append(leave.getStartDate())
                .append(" to ").append(leave.getEndDate())
                .append(" has been ").append(decision.toLowerCase(Locale.ROOT)).append(".");
        if (reviewComment != null && !reviewComment.isBlank()) {
            message.append(" Review comment: ").append(reviewComment.trim());
        }

        if (notificationService != null) {
            notificationService.createNotification(
                    employee.getId(),
                    subject,
                    message.toString(),
                    "LEAVE",
                    sender,
                    role
            );
        }

        if (emailNotificationService != null) {
            emailNotificationService.send(
                    employee,
                    subject,
                    message + "\n\nSent by: " + sender + " (" + role + ")\nNexusHR"
            );
        }
    }

}
