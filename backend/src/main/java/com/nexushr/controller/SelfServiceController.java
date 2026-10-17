package com.nexushr.controller;

import com.nexushr.dto.AttendanceDTO;
import com.nexushr.dto.EmployeeDocumentDTO;
import com.nexushr.dto.LeaveBalanceDTO;
import com.nexushr.dto.LeaveDTO;
import com.nexushr.dto.NotificationDTO;

import com.nexushr.entity.Employee;
import com.nexushr.entity.EmployeeDocument;
import com.nexushr.entity.Payroll;
import com.nexushr.entity.PerformanceGoal;
import com.nexushr.entity.PerformanceFeedback;
import com.nexushr.entity.PerformanceReview;
import com.nexushr.entity.User;

import com.nexushr.repository.EmployeeRepository;
import com.nexushr.repository.UserRepository;

import com.nexushr.service.AttendanceService;
import com.nexushr.service.EmployeeDocumentService;
import com.nexushr.service.LeaveService;
import com.nexushr.service.NotificationService;
import com.nexushr.service.PayrollService;
import com.nexushr.service.PerformanceReviewService;
import com.nexushr.service.PerformanceGoalService;
import com.nexushr.service.PerformanceFeedbackService;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Year;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/me")
public class SelfServiceController {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final AttendanceService attendanceService;
    private final EmployeeDocumentService employeeDocumentService;
    private final LeaveService leaveService;
    private final PayrollService payrollService;
    private final PerformanceReviewService performanceReviewService;
    private final PerformanceGoalService performanceGoalService;
    private final PerformanceFeedbackService performanceFeedbackService;
    private final NotificationService notificationService;

    public SelfServiceController(
            EmployeeRepository employeeRepository,
            UserRepository userRepository,
            AttendanceService attendanceService,
            EmployeeDocumentService employeeDocumentService,
            LeaveService leaveService,
            PayrollService payrollService,
            PerformanceReviewService performanceReviewService,
            PerformanceGoalService performanceGoalService,
            PerformanceFeedbackService performanceFeedbackService,
            NotificationService notificationService
    ) {
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.attendanceService = attendanceService;
        this.employeeDocumentService = employeeDocumentService;
        this.leaveService = leaveService;
        this.payrollService = payrollService;
        this.performanceReviewService = performanceReviewService;
        this.performanceGoalService = performanceGoalService;
        this.performanceFeedbackService = performanceFeedbackService;
        this.notificationService = notificationService;
    }

    // =========================================================
    // CURRENT EMPLOYEE
    // GET /api/me
    // =========================================================

    @GetMapping
    public ResponseEntity<Map<String, Object>> getCurrentEmployee(
            Authentication authentication
    ) {

        Employee employee =
                getAuthenticatedEmployee(authentication);

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put(
                "employeeId",
                employee.getId()
        );

        response.put(
                "username",
                authentication.getName()
        );

        response.put(
                "firstName",
                employee.getFirstName()
        );

        response.put(
                "lastName",
                employee.getLastName()
        );

        response.put(
                "fullName",
                buildEmployeeName(employee)
        );

        response.put(
                "email",
                employee.getEmail()
        );

        response.put(
                "phone",
                employee.getPhone()
        );

        response.put(
                "department",
                employee.getDepartment()
        );

        response.put(
                "jobTitle",
                employee.getJobTitle()
        );

        response.put(
                "joiningDate",
                employee.getJoiningDate()
        );

        response.put(
                "employmentStatus",
                employee.getEmploymentStatus()
        );

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // ATTENDANCE
    // =========================================================

    @GetMapping("/attendance")
    public ResponseEntity<List<AttendanceDTO>>
    getMyAttendance(
            Authentication authentication
    ) {

        Employee employee =
                getAuthenticatedEmployee(authentication);

        return ResponseEntity.ok(
                attendanceService
                        .getEmployeeAttendance(
                                employee.getId()
                        )
        );
    }

    @PostMapping("/attendance/check-in")
    public ResponseEntity<AttendanceDTO>
    checkIn(
            Authentication authentication
    ) {

        Employee employee =
                getAuthenticatedEmployee(authentication);

        return ResponseEntity.ok(
                attendanceService.checkIn(
                        employee.getId()
                )
        );
    }

    @PostMapping("/attendance/check-out")
    public ResponseEntity<AttendanceDTO>
    checkOut(
            Authentication authentication
    ) {

        Employee employee =
                getAuthenticatedEmployee(authentication);

        return ResponseEntity.ok(
                attendanceService.checkOut(
                        employee.getId()
                )
        );
    }

    @PostMapping("/attendance/half-day")
    public ResponseEntity<AttendanceDTO>
    markHalfDay(Authentication authentication) {
        Employee employee = getAuthenticatedEmployee(authentication);
        return ResponseEntity.ok(
                attendanceService.markHalfDay(employee.getId(), LocalDate.now())
        );
    }

    // =========================================================
    // LEAVE
    // =========================================================

    @GetMapping("/leaves")
    public ResponseEntity<List<LeaveDTO>>
    getMyLeaves(
            Authentication authentication
    ) {

        Employee employee =
                getAuthenticatedEmployee(authentication);

        return ResponseEntity.ok(
                leaveService.getEmployeeLeaves(
                        employee.getId(),
                        authentication
                )
        );
    }

    @GetMapping("/leaves/balance")
    public ResponseEntity<List<LeaveBalanceDTO>>
    getMyLeaveBalances(
            @RequestParam(required = false)
            Integer year,

            Authentication authentication
    ) {

        Employee employee =
                getAuthenticatedEmployee(authentication);

        int requestedYear =
                year != null
                        ? year
                        : Year.now().getValue();

        return ResponseEntity.ok(
                leaveService
                        .getEmployeeLeaveBalances(
                                employee.getId(),
                                requestedYear,
                                authentication
                        )
        );
    }

    @PostMapping("/leaves")
    public ResponseEntity<LeaveDTO>
    createMyLeave(
            @RequestBody LeaveRequest request,
            Authentication authentication
    ) {

        Employee employee =
                getAuthenticatedEmployee(authentication);

        LeaveDTO dto =
                new LeaveDTO();

        dto.setEmployeeId(
                employee.getId()
        );

        dto.setLeaveType(
                request.leaveType()
        );

        dto.setStartDate(
                request.startDate()
        );

        dto.setEndDate(
                request.endDate()
        );

        dto.setReason(
                request.reason()
        );

        LeaveDTO created =
                leaveService.createLeave(
                        dto,
                        authentication
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(created);
    }

    @PutMapping("/leaves/{leaveId}/cancel")
    public ResponseEntity<LeaveDTO>
    cancelMyLeave(
            @PathVariable Long leaveId,
            Authentication authentication
    ) {

        Employee employee =
                getAuthenticatedEmployee(authentication);

        boolean ownsLeave =
                leaveService
                        .getEmployeeLeaves(
                                employee.getId(),
                                authentication
                        )
                        .stream()
                        .anyMatch(
                                leave ->
                                        leaveId.equals(
                                                leave.getId()
                                        )
                        );

        if (!ownsLeave) {
            throw new RuntimeException(
                    "Leave request not found"
            );
        }

        return ResponseEntity.ok(
                leaveService.cancelLeave(
                        leaveId,
                        authentication
                )
        );
    }

    // =========================================================
    // PAYROLL
    // =========================================================

    @GetMapping("/payroll")
    public ResponseEntity<?> getMyPayroll(
            Authentication authentication
    ) {

        Employee employee =
                getAuthenticatedEmployee(authentication);

        return ResponseEntity.ok(
                payrollService
                        .getPayrollsByEmployee(
                                employee.getId()
                        )
        );
    }


    // =========================================================
    // EMPLOYEE DOCUMENTS
    // =========================================================

    @GetMapping("/documents")
    public ResponseEntity<List<EmployeeDocumentDTO>>
    getMyDocuments(
            Authentication authentication
    ) {

        Employee employee =
                getAuthenticatedEmployee(authentication);

        return ResponseEntity.ok(
                employeeDocumentService
                        .getEmployeeDocuments(
                                employee.getId()
                        )
        );
    }

    @PostMapping(
            value = "/documents",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<EmployeeDocumentDTO>
    uploadMyDocument(
            @RequestParam("documentName")
            String documentName,

            @RequestParam("documentType")
            String documentType,

            @RequestParam("file")
            MultipartFile file,

            Authentication authentication
    ) throws IOException {

        Employee employee =
                getAuthenticatedEmployee(authentication);

        EmployeeDocumentDTO created =
                employeeDocumentService
                        .uploadDocument(
                                employee.getId(),
                                documentName,
                                documentType,
                                file
                        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(created);
    }

    @GetMapping("/documents/{documentId}/download")
    public ResponseEntity<ByteArrayResource>
    downloadMyDocument(
            @PathVariable Long documentId,
            Authentication authentication
    ) {

        Employee employee =
                getAuthenticatedEmployee(authentication);

        EmployeeDocument document =
                employeeDocumentService
                        .getDocument(
                                employee.getId(),
                                documentId
                        );

        ByteArrayResource resource =
                new ByteArrayResource(
                        document.getFileData()
                );

        MediaType mediaType;

        try {
            mediaType =
                    document.getContentType() == null
                            ? MediaType.APPLICATION_OCTET_STREAM
                            : MediaType.parseMediaType(
                            document.getContentType()
                    );
        } catch (Exception exception) {
            mediaType =
                    MediaType.APPLICATION_OCTET_STREAM;
        }

        String filename =
                sanitizeFilename(
                        document.getFileName(),
                        "document-" + documentId
                );

        return ResponseEntity
                .ok()
                .contentType(mediaType)
                .contentLength(document.getFileSize())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" +
                                filename +
                                "\""
                )
                .body(resource);
    }

    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<Void>
    deleteMyDocument(
            @PathVariable Long documentId,
            Authentication authentication
    ) {

        Employee employee =
                getAuthenticatedEmployee(authentication);

        employeeDocumentService
                .deleteDocument(
                        employee.getId(),
                        documentId
                );

        return ResponseEntity
                .noContent()
                .build();
    }


    @GetMapping(
            value = "/payroll/{payrollId}/payslip",
            produces = MediaType.APPLICATION_PDF_VALUE
    )
    public ResponseEntity<byte[]>
    downloadMyPayslip(
            @PathVariable Long payrollId,
            Authentication authentication
    ) {

        Employee employee =
                getAuthenticatedEmployee(authentication);

        Payroll payroll =
                payrollService
                        .getPayrollById(payrollId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Payroll record not found"
                                )
                        );

        if (payroll.getEmployee() == null ||
                payroll.getEmployee().getId() == null ||
                !employee.getId().equals(
                        payroll.getEmployee().getId()
                )) {

            throw new RuntimeException(
                    "Payroll record not found"
            );
        }

        byte[] pdf =
                payrollService
                        .generatePayslip(
                                payrollId
                        );

        String employeeName =
                sanitizeFilename(
                        buildEmployeeName(employee),
                        "employee"
                );

        String month =
                payroll.getPayrollMonth() == null
                        ? "payroll"
                        : payroll
                        .getPayrollMonth()
                        .toString();

        String filename =
                "NexusHR-Payslip-" +
                        employeeName +
                        "-" +
                        month +
                        ".pdf";

        return ResponseEntity
                .ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" +
                                filename +
                                "\""
                )
                .contentType(
                        MediaType.APPLICATION_PDF
                )
                .contentLength(
                        pdf.length
                )
                .body(pdf);
    }

    // =========================================================
    // PERFORMANCE
    // =========================================================

    @GetMapping("/performance")
    public ResponseEntity<?> getMyPerformance(
            Authentication authentication
    ) {

        Employee employee =
                getAuthenticatedEmployee(authentication);

        return ResponseEntity.ok(
                performanceReviewService
                        .getReviewsByEmployee(
                                employee.getId()
                        )
        );
    }

    @GetMapping("/performance/goals")
    public ResponseEntity<List<PerformanceGoal>> getMyPerformanceGoals(
            Authentication authentication
    ) {
        Employee employee = getAuthenticatedEmployee(authentication);
        return ResponseEntity.ok(
                performanceGoalService.getByEmployee(employee.getId())
        );
    }

    @PatchMapping("/performance/goals/{goalId}/progress")
    public ResponseEntity<PerformanceGoal> updateMyGoalProgress(
            @PathVariable Long goalId,
            @RequestBody GoalProgressRequest request,
            Authentication authentication
    ) {
        Employee employee = getAuthenticatedEmployee(authentication);
        boolean ownsGoal = performanceGoalService.getByEmployee(employee.getId())
                .stream().anyMatch(goal -> goalId.equals(goal.getId()));
        if (!ownsGoal) throw new RuntimeException("Performance goal not found");
        return ResponseEntity.ok(
                performanceGoalService.updateProgress(goalId, request.progress(), request.employeeComment())
        );
    }

    @GetMapping("/performance/feedback")
    public ResponseEntity<List<PerformanceFeedback>> getMyPerformanceFeedback(
            Authentication authentication
    ) {
        Employee employee = getAuthenticatedEmployee(authentication);
        return ResponseEntity.ok(
                performanceFeedbackService.getFeedbackByEmployee(employee.getId())
        );
    }

    @PostMapping("/performance/{reviewId}/self-feedback")
    public ResponseEntity<PerformanceFeedback> submitMySelfFeedback(
            @PathVariable Long reviewId,
            @RequestBody SelfFeedbackRequest request,
            Authentication authentication
    ) {
        Employee employee = getAuthenticatedEmployee(authentication);
        boolean ownsReview = performanceReviewService.getReviewsByEmployee(employee.getId())
                .stream().anyMatch(review -> reviewId.equals(review.getId()));
        if (!ownsReview) throw new RuntimeException("Performance review not found");

        PerformanceFeedback feedback = new PerformanceFeedback();
        PerformanceReview review = new PerformanceReview();
        review.setId(reviewId);
        feedback.setReview(review);
        feedback.setEmployee(employee);
        feedback.setReviewer(employee);
        feedback.setFeedbackType(PerformanceFeedback.FeedbackType.SELF);
        feedback.setRating(request.rating());
        feedback.setComments(request.comments());
        feedback.setStrengths(request.strengths());
        feedback.setAreasForImprovement(request.areasForImprovement());
        return ResponseEntity.ok(performanceFeedbackService.createFeedback(feedback));
    }

    // =========================================================
    // NOTIFICATIONS
    // =========================================================

    @GetMapping("/notifications")
    public ResponseEntity<List<NotificationDTO>>
    getMyNotifications(
            Authentication authentication
    ) {

        Employee employee =
                getAuthenticatedEmployee(authentication);

        return ResponseEntity.ok(
                notificationService
                        .getEmployeeNotifications(
                                employee.getId()
                        )
        );
    }

    @GetMapping("/notifications/unread-count")
    public ResponseEntity<Map<String, Long>>
    getMyUnreadNotificationCount(
            Authentication authentication
    ) {

        Employee employee =
                getAuthenticatedEmployee(authentication);

        long count =
                notificationService
                        .countUnreadNotifications(
                                employee.getId()
                        );

        return ResponseEntity.ok(
                Map.of(
                        "unreadCount",
                        count
                )
        );
    }

    @PutMapping("/notifications/read-all")
    public ResponseEntity<Map<String, Integer>>
    markAllMyNotificationsRead(
            Authentication authentication
    ) {

        Employee employee =
                getAuthenticatedEmployee(authentication);

        int updated =
                notificationService
                        .markAllAsRead(
                                employee.getId()
                        );

        return ResponseEntity.ok(
                Map.of(
                        "updatedCount",
                        updated
                )
        );
    }

    public record GoalProgressRequest(int progress, String employeeComment) {}

    public record SelfFeedbackRequest(
            Integer rating,
            String comments,
            String strengths,
            String areasForImprovement
    ) {}

    // =========================================================
    // CURRENT EMPLOYEE RESOLUTION
    // =========================================================

    private Employee getAuthenticatedEmployee(
            Authentication authentication
    ) {

        if (authentication == null ||
                !authentication.isAuthenticated()) {

            throw new RuntimeException(
                    "Authentication is required"
            );
        }

        String username =
                authentication.getName();

        // Fast path: the normal one-to-one account link already exists.
        var linkedEmployee = employeeRepository
                .findByUserUsername(username);

        if (linkedEmployee.isPresent()) {
            return linkedEmployee.get();
        }

        User user = userRepository
                .findByUsernameIgnoreCase(username)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Authenticated user account was not found: "
                                        + username
                        )
                );

        // Repair older/demo accounts automatically when the account e-mail
        // (or an e-mail used as username) matches an existing employee row.
        // This is especially important for employee self-service attendance.
        String recoveryEmail = user.getRecoveryEmail();
        String usernameCandidate = username == null ? "" : username.trim();

        var matchingEmployee = recoveryEmail != null && !recoveryEmail.isBlank()
                ? employeeRepository.findByEmailIgnoreCase(recoveryEmail.trim())
                : java.util.Optional.<Employee>empty();

        if (matchingEmployee.isEmpty() && usernameCandidate.contains("@")) {
            matchingEmployee = employeeRepository.findByEmailIgnoreCase(usernameCandidate);
        }

        if (matchingEmployee.isPresent()) {
            Employee employee = matchingEmployee.get();

            if (employee.getUser() == null ||
                    java.util.Objects.equals(employee.getUser().getId(), user.getId())) {
                employee.setUser(user);
                return employeeRepository.save(employee);
            }
        }

        // Managers and employees are both workforce members. Older/demo
        // databases may contain a login account without the corresponding
        // Employee row, which makes self-service attendance unusable. Repair
        // only MANAGER/EMPLOYEE accounts; ADMIN/HR users are never silently
        // converted into employees.
        String role = user.getRole() == null
                ? ""
                : user.getRole().trim().toUpperCase();

        if ("MANAGER".equals(role) || "EMPLOYEE".equals(role)) {
            Employee employee = new Employee();

            String fallbackName = "MANAGER".equals(role)
                    ? "Manager"
                    : "Employee";

            String displayName = username == null || username.isBlank()
                    ? fallbackName
                    : username.trim();

            String cleanDisplayName = displayName
                    .replaceAll("[^A-Za-z0-9._ -]", "")
                    .trim();

            if (cleanDisplayName.isBlank()) {
                cleanDisplayName = fallbackName;
            }

            employee.setFirstName(
                    Character.toUpperCase(cleanDisplayName.charAt(0))
                            + cleanDisplayName.substring(1)
            );
            employee.setLastName("");

            String generatedLocalPart = cleanDisplayName
                    .toLowerCase()
                    .replaceAll("[^a-z0-9._-]", "");

            if (generatedLocalPart.isBlank()) {
                generatedLocalPart = role.toLowerCase();
            }

            String generatedEmail = generatedLocalPart
                    + "-" + user.getId()
                    + "@nexushr.local";

            String employeeEmail = recoveryEmail != null
                    && !recoveryEmail.isBlank()
                    && employeeRepository.findByEmail(recoveryEmail.trim()).isEmpty()
                    ? recoveryEmail.trim()
                    : generatedEmail;

            if (employeeRepository.findByEmail(employeeEmail).isPresent()) {
                employeeEmail = role.toLowerCase()
                        + "-" + user.getId()
                        + "@nexushr.local";
            }

            employee.setEmail(employeeEmail);
            employee.setPhone(null);
            employee.setDepartment(
                    "MANAGER".equals(role)
                            ? "Management"
                            : "General"
            );
            employee.setJobTitle(
                    "MANAGER".equals(role)
                            ? "Manager"
                            : "Employee"
            );
            employee.setJoiningDate(LocalDate.now());
            employee.setSalary(0.0);
            employee.setEmploymentStatus("ACTIVE");
            employee.setApprovalStatus("APPROVED");
            employee.setUser(user);

            return employeeRepository.save(employee);
        }

        throw new RuntimeException(
                "No employee profile is linked to user: " + username
        );
    }

    private String buildEmployeeName(
            Employee employee
    ) {

        String firstName =
                employee.getFirstName() == null
                        ? ""
                        : employee.getFirstName().trim();

        String lastName =
                employee.getLastName() == null
                        ? ""
                        : employee.getLastName().trim();

        return (
                firstName + " " + lastName
        ).trim();
    }

    private String sanitizeFilename(
            String value,
            String fallback
    ) {

        if (value == null ||
                value.isBlank()) {

            return fallback;
        }

        String sanitized =
                value.trim()
                        .replaceAll(
                                "\\s+",
                                "-"
                        )
                        .replaceAll(
                                "[^a-zA-Z0-9._-]",
                                ""
                        );

        return sanitized.isBlank()
                ? fallback
                : sanitized;
    }

    public record LeaveRequest(
            String leaveType,
            LocalDate startDate,
            LocalDate endDate,
            String reason
    ) {
    }
}
