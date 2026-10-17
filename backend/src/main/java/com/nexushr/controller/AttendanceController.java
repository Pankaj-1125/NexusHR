package com.nexushr.controller;

import com.nexushr.dto.AttendanceDTO;
import com.nexushr.dto.AttendanceDashboardDTO;
import com.nexushr.service.AttendanceService;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(
            AttendanceService attendanceService
    ) {
        this.attendanceService = attendanceService;
    }

    /*
     * =========================================================
     * CREATE ATTENDANCE
     * POST /api/attendance
     * =========================================================
     */

    @PostMapping
    public ResponseEntity<AttendanceDTO> createAttendance(
            @Valid @RequestBody AttendanceDTO attendanceDTO
    ) {

        return ResponseEntity.ok(
                attendanceService.createAttendance(
                        attendanceDTO
                )
        );
    }

    /*
     * =========================================================
     * CHECK IN
     * POST /api/attendance/check-in/{employeeId}
     * =========================================================
     */

    @PostMapping("/check-in/{employeeId}")
    public ResponseEntity<AttendanceDTO> checkIn(
            @PathVariable Long employeeId
    ) {

        return ResponseEntity.ok(
                attendanceService.checkIn(
                        employeeId
                )
        );
    }

    /*
     * =========================================================
     * BIOMETRIC CHECK IN SIMULATION
     * POST /api/attendance/biometric/check-in/{employeeId}
     * =========================================================
     */

    @PostMapping("/biometric/check-in/{employeeId}")
    public ResponseEntity<AttendanceDTO> biometricCheckIn(
            @PathVariable Long employeeId
    ) {

        return ResponseEntity.ok(
                attendanceService.checkIn(
                        employeeId
                )
        );
    }

    /*
     * =========================================================
     * CHECK OUT
     * POST /api/attendance/check-out/{employeeId}
     * =========================================================
     */

    @PostMapping("/check-out/{employeeId}")
    public ResponseEntity<AttendanceDTO> checkOut(
            @PathVariable Long employeeId
    ) {

        return ResponseEntity.ok(
                attendanceService.checkOut(
                        employeeId
                )
        );
    }

    /*
     * =========================================================
     * BIOMETRIC CHECK OUT SIMULATION
     * POST /api/attendance/biometric/check-out/{employeeId}
     * =========================================================
     */

    @PostMapping("/biometric/check-out/{employeeId}")
    public ResponseEntity<AttendanceDTO> biometricCheckOut(
            @PathVariable Long employeeId
    ) {

        return ResponseEntity.ok(
                attendanceService.checkOut(
                        employeeId
                )
        );
    }

    /*
     * =========================================================
     * GET ALL ATTENDANCE
     * GET /api/attendance
     * =========================================================
     */

    @GetMapping
    public ResponseEntity<List<AttendanceDTO>> getAllAttendance() {

        return ResponseEntity.ok(
                attendanceService.getAllAttendance()
        );
    }

    @PostMapping("/half-day/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<AttendanceDTO> markHalfDay(
            @PathVariable Long employeeId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(attendanceService.markHalfDay(employeeId, date));
    }

    /*
     * =========================================================
     * ATTENDANCE DASHBOARD
     *
     * GET /api/attendance/dashboard
     * GET /api/attendance/dashboard?date=2026-10-03
     * =========================================================
     */

    @GetMapping("/dashboard")
    public ResponseEntity<AttendanceDashboardDTO>
    getAttendanceDashboard(

            @RequestParam(
                    required = false
            )

            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )

            LocalDate date
    ) {

        return ResponseEntity.ok(
                attendanceService
                        .getAttendanceDashboard(
                                date
                        )
        );
    }

    /*
     * =========================================================
     * GET ATTENDANCE BY ID
     * GET /api/attendance/{id}
     * =========================================================
     */

    @GetMapping("/{id}")
    public ResponseEntity<AttendanceDTO> getAttendanceById(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                attendanceService
                        .getAttendanceById(
                                id
                        )
        );
    }

    /*
     * =========================================================
     * GET EMPLOYEE ATTENDANCE
     * GET /api/attendance/employee/{employeeId}
     * =========================================================
     */

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<AttendanceDTO>>
    getEmployeeAttendance(
            @PathVariable Long employeeId
    ) {

        return ResponseEntity.ok(
                attendanceService
                        .getEmployeeAttendance(
                                employeeId
                        )
        );
    }

    /*
     * =========================================================
     * GET ATTENDANCE BY DATE
     * GET /api/attendance/date/{date}
     *
     * Example:
     * /api/attendance/date/2026-10-03
     * =========================================================
     */

    @GetMapping("/date/{date}")
    public ResponseEntity<List<AttendanceDTO>>
    getAttendanceByDate(

            @PathVariable

            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )

            LocalDate date
    ) {

        return ResponseEntity.ok(
                attendanceService
                        .getAttendanceByDate(
                                date
                        )
        );
    }

    /*
     * =========================================================
     * GET EMPLOYEE ATTENDANCE BETWEEN DATES
     *
     * GET
     * /api/attendance/employee/{employeeId}/between
     * ?startDate=2026-10-01
     * &endDate=2026-10-31
     * =========================================================
     */

    @GetMapping(
            "/employee/{employeeId}/between"
    )
    public ResponseEntity<List<AttendanceDTO>>
    getEmployeeAttendanceBetweenDates(

            @PathVariable
            Long employeeId,

            @RequestParam
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate startDate,

            @RequestParam
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate endDate
    ) {

        return ResponseEntity.ok(
                attendanceService
                        .getEmployeeAttendanceBetweenDates(
                                employeeId,
                                startDate,
                                endDate
                        )
        );
    }

    /*
     * =========================================================
     * GET ATTENDANCE BY STATUS
     * GET /api/attendance/status/{status}
     * =========================================================
     */

    @GetMapping("/status/{status}")
    public ResponseEntity<List<AttendanceDTO>>
    getAttendanceByStatus(
            @PathVariable String status
    ) {

        return ResponseEntity.ok(
                attendanceService
                        .getAttendanceByStatus(
                                status
                        )
        );
    }

    /*
     * =========================================================
     * UPDATE ATTENDANCE
     * PUT /api/attendance/{id}
     * =========================================================
     */

    @PutMapping("/{id}")
    public ResponseEntity<AttendanceDTO> updateAttendance(

            @PathVariable
            Long id,

            @RequestBody
            AttendanceDTO attendanceDTO
    ) {

        return ResponseEntity.ok(
                attendanceService
                        .updateAttendance(
                                id,
                                attendanceDTO
                        )
        );
    }

    /*
     * =========================================================
     * DELETE ATTENDANCE
     * DELETE /api/attendance/{id}
     * =========================================================
     */

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAttendance(
            @PathVariable Long id
    ) {

        attendanceService.deleteAttendance(
                id
        );

        return ResponseEntity
                .noContent()
                .build();
    }
}
