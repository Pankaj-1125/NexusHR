package com.nexushr.controller;

import com.nexushr.dto.LeaveBalanceDTO;
import com.nexushr.dto.LeaveDTO;
import com.nexushr.service.LeaveService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.Year;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leaves")
public class LeaveController {

    private final LeaveService leaveService;

    public LeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    // =========================================================
    // CREATE
    // =========================================================

    @PostMapping
    public ResponseEntity<LeaveDTO> createLeave(
            @Valid @RequestBody LeaveDTO dto,
            Authentication authentication) {

        LeaveDTO createdLeave =
                leaveService.createLeave(
                        dto,
                        authentication
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdLeave);
    }

    // =========================================================
    // READ ALL
    // =========================================================

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<List<LeaveDTO>> getAllLeaves() {

        return ResponseEntity.ok(
                leaveService.getAllLeaves()
        );
    }

    // =========================================================
    // READ BY ID
    // =========================================================

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<LeaveDTO> getLeaveById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                leaveService.getLeaveById(id)
        );
    }

    // =========================================================
    // READ EMPLOYEE LEAVES
    // =========================================================

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<LeaveDTO>> getEmployeeLeaves(
            @PathVariable Long employeeId,
            Authentication authentication) {

        return ResponseEntity.ok(
                leaveService.getEmployeeLeaves(
                        employeeId,
                        authentication
                )
        );
    }

    // =========================================================
    // LEAVE BALANCE - ALL TYPES
    // =========================================================

    @GetMapping("/employee/{employeeId}/balance")
    public ResponseEntity<List<LeaveBalanceDTO>> getEmployeeLeaveBalances(
            @PathVariable Long employeeId,
            @RequestParam(required = false) Integer year,
            Authentication authentication) {

        int balanceYear =
                year != null
                        ? year
                        : Year.now().getValue();

        return ResponseEntity.ok(
                leaveService.getEmployeeLeaveBalances(
                        employeeId,
                        balanceYear,
                        authentication
                )
        );
    }

    // =========================================================
    // LEAVE BALANCE - SPECIFIC TYPE
    // =========================================================

    @GetMapping("/employee/{employeeId}/balance/{leaveType}")
    public ResponseEntity<LeaveBalanceDTO> getLeaveBalance(
            @PathVariable Long employeeId,
            @PathVariable String leaveType,
            @RequestParam(required = false) Integer year,
            Authentication authentication) {

        int balanceYear =
                year != null
                        ? year
                        : Year.now().getValue();

        return ResponseEntity.ok(
                leaveService.getLeaveBalance(
                        employeeId,
                        balanceYear,
                        leaveType,
                        authentication
                )
        );
    }

    // =========================================================
    // FILTER BY STATUS
    // =========================================================

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<List<LeaveDTO>> getLeavesByStatus(
            @PathVariable String status) {

        return ResponseEntity.ok(
                leaveService.getLeavesByStatus(
                        status
                )
        );
    }

    // =========================================================
    // FILTER BY TYPE
    // =========================================================

    @GetMapping("/type/{leaveType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<List<LeaveDTO>> getLeavesByType(
            @PathVariable String leaveType) {

        return ResponseEntity.ok(
                leaveService.getLeavesByType(
                        leaveType
                )
        );
    }

    // =========================================================
    // APPROVE
    // =========================================================

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<LeaveDTO> approveLeave(
            @PathVariable Long id,
            @RequestParam(required = false) String reviewComment,
            Authentication authentication) {

        return ResponseEntity.ok(
                leaveService.approveLeave(
                        id,
                        reviewComment,
                        authentication
                )
        );
    }

    // =========================================================
    // REJECT
    // =========================================================

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<LeaveDTO> rejectLeave(
            @PathVariable Long id,
            @RequestParam(required = false) String reviewComment,
            Authentication authentication) {

        return ResponseEntity.ok(
                leaveService.rejectLeave(
                        id,
                        reviewComment,
                        authentication
                )
        );
    }

    // =========================================================
    // CANCEL
    // =========================================================

    @PutMapping("/{id}/cancel")
    public ResponseEntity<LeaveDTO> cancelLeave(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(
                leaveService.cancelLeave(
                        id,
                        authentication
                )
        );
    }

    // =========================================================
    // UPDATE
    // =========================================================

    @PutMapping("/{id}")
    public ResponseEntity<LeaveDTO> updateLeave(
            @PathVariable Long id,
            @Valid @RequestBody LeaveDTO dto,
            Authentication authentication) {

        return ResponseEntity.ok(
                leaveService.updateLeave(
                        id,
                        dto,
                        authentication
                )
        );
    }

    // =========================================================
    // DELETE
    // =========================================================

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteLeave(
            @PathVariable Long id,
            Authentication authentication) {

        leaveService.deleteLeave(
                id,
                authentication
        );

        Map<String, String> response =
                new HashMap<>();

        response.put(
                "message",
                "Leave request deleted successfully."
        );

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // ALL LEAVES BETWEEN DATES
    // =========================================================

    @GetMapping("/between")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<List<LeaveDTO>> getLeavesBetweenDates(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {

        return ResponseEntity.ok(
                leaveService.getLeavesBetweenDates(
                        startDate,
                        endDate
                )
        );
    }

    // =========================================================
    // EMPLOYEE LEAVES BETWEEN DATES
    // =========================================================

    @GetMapping("/employee/{employeeId}/between")
    public ResponseEntity<List<LeaveDTO>> getEmployeeLeavesBetweenDates(
            @PathVariable Long employeeId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            Authentication authentication) {

        return ResponseEntity.ok(
                leaveService.getEmployeeLeavesBetweenDates(
                        employeeId,
                        startDate,
                        endDate,
                        authentication
                )
        );
    }

    // =========================================================
    // COUNT BY STATUS
    // =========================================================

    @GetMapping("/count/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> countLeavesByStatus(
            @PathVariable String status) {

        long count =
                leaveService.countLeavesByStatus(
                        status
                );

        Map<String, Object> response =
                new HashMap<>();

        response.put(
                "status",
                status.toUpperCase()
        );

        response.put(
                "count",
                count
        );

        return ResponseEntity.ok(response);
    }
}




