package com.nexushr.controller;

import com.nexushr.dto.EmployeeDTO;
import com.nexushr.service.EmployeeService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(
            EmployeeService employeeService
    ) {

        this.employeeService =
                employeeService;
    }

    // =========================================================
    // CREATE
    // ADMIN + HR
    // =========================================================

    @PostMapping
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'HR')"
    )
    public ResponseEntity<EmployeeDTO> addEmployee(
            @Valid
            @RequestBody EmployeeDTO employeeDTO
    ) {

        EmployeeDTO created =
                employeeService.addEmployee(
                        employeeDTO
                );

        return ResponseEntity
                .status(
                        HttpStatus.CREATED
                )
                .body(
                        created
                );
    }

    // =========================================================
    // MANUAL ONBOARDING DISABLED
    // =========================================================

    @PostMapping("/{id}/onboard")
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'HR')"
    )
    public ResponseEntity<String> onboardEmployee(
            @PathVariable Long id
    ) {

        return ResponseEntity
                .badRequest()
                .body(
                        "Manual username assignment is disabled. "
                                + "Employee must use registered email, OTP, "
                                + "and choose their username during first-time setup."
                );
    }

    // =========================================================
    // APPROVE
    // =========================================================

    @PostMapping("/{id}/approve")
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'HR')"
    )
    public ResponseEntity<EmployeeDTO> approveEmployee(
            @PathVariable Long id
    ) {

        try {

            return ResponseEntity.ok(
                    employeeService
                            .approveEmployee(
                                    id
                            )
            );

        } catch (RuntimeException exception) {

            return ResponseEntity
                    .badRequest()
                    .build();
        }
    }

    // =========================================================
    // REJECT
    // =========================================================

    @PostMapping("/{id}/reject")
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'HR')"
    )
    public ResponseEntity<EmployeeDTO> rejectEmployee(
            @PathVariable Long id
    ) {

        try {

            return ResponseEntity.ok(
                    employeeService
                            .rejectEmployee(
                                    id
                            )
            );

        } catch (RuntimeException exception) {

            return ResponseEntity
                    .badRequest()
                    .build();
        }
    }

    // =========================================================
    // OFFBOARD
    // =========================================================

    @PostMapping("/{id}/offboard")
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'HR')"
    )
    public ResponseEntity<EmployeeDTO> offboardEmployee(
            @PathVariable Long id
    ) {

        try {

            return ResponseEntity.ok(
                    employeeService
                            .offboardEmployee(
                                    id
                            )
            );

        } catch (RuntimeException exception) {

            return ResponseEntity
                    .notFound()
                    .build();
        }
    }

    // =========================================================
    // REACTIVATE
    // =========================================================

    @PostMapping("/{id}/reactivate")
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'HR')"
    )
    public ResponseEntity<EmployeeDTO> reactivateEmployee(
            @PathVariable Long id
    ) {

        try {

            return ResponseEntity.ok(
                    employeeService
                            .reactivateEmployee(
                                    id
                            )
            );

        } catch (RuntimeException exception) {

            return ResponseEntity
                    .notFound()
                    .build();
        }
    }

    // =========================================================
    // GET ALL
    // =========================================================

    @GetMapping
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'HR', 'MANAGER')"
    )
    public ResponseEntity<List<EmployeeDTO>>
    getAllEmployees() {

        return ResponseEntity.ok(
                employeeService
                        .getAllEmployees()
        );
    }

    // =========================================================
    // GET BY ID
    // =========================================================

    @GetMapping("/{id}")
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'HR', 'MANAGER')"
    )
    public ResponseEntity<EmployeeDTO> getEmployeeById(
            @PathVariable Long id
    ) {

        try {

            return ResponseEntity.ok(
                    employeeService
                            .getEmployeeById(
                                    id
                            )
            );

        } catch (RuntimeException exception) {

            return ResponseEntity
                    .notFound()
                    .build();
        }
    }

    // =========================================================
    // GET BY EMAIL
    // =========================================================

    @GetMapping("/email/{email}")
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'HR', 'MANAGER')"
    )
    public ResponseEntity<EmployeeDTO> getEmployeeByEmail(
            @PathVariable String email
    ) {

        try {

            return ResponseEntity.ok(
                    employeeService
                            .getEmployeeByEmail(
                                    email
                            )
            );

        } catch (RuntimeException exception) {

            return ResponseEntity
                    .notFound()
                    .build();
        }
    }

    // =========================================================
    // UPDATE
    // ADMIN + HR + MANAGER (existing employee edit only)
    // =========================================================

    @PutMapping("/{id}")
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'HR', 'MANAGER')"
    )
    public ResponseEntity<EmployeeDTO> updateEmployee(
            @PathVariable Long id,
            @Valid
            @RequestBody EmployeeDTO employeeDTO
    ) {

        try {

            return ResponseEntity.ok(
                    employeeService
                            .updateEmployee(
                                    id,
                                    employeeDTO
                            )
            );

        } catch (RuntimeException exception) {

            return ResponseEntity
                    .badRequest()
                    .build();
        }
    }

    // =========================================================
    // PERMANENT DELETE
    // ADMIN ONLY
    // =========================================================

    @DeleteMapping("/{id}")
    @PreAuthorize(
            "hasRole('ADMIN')"
    )
    public ResponseEntity<java.util.Map<String, String>> deleteEmployee(
            @PathVariable Long id
    ) {

        try {

            employeeService.deleteEmployee(id);

            return ResponseEntity.ok(
                    java.util.Map.of(
                            "message",
                            "Employee and all linked data deleted successfully"
                    )
            );

        } catch (RuntimeException exception) {

            exception.printStackTrace();

            String message =
                    exception.getMessage() == null
                            || exception.getMessage().isBlank()
                            ? "Employee deletion failed"
                            : exception.getMessage();

            return ResponseEntity
                    .badRequest()
                    .body(
                            java.util.Map.of(
                                    "message",
                                    message
                            )
                    );
        }
    }

    // =========================================================
    // SEARCH
    // =========================================================

    @GetMapping("/search")
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'HR', 'MANAGER')"
    )
    public ResponseEntity<List<EmployeeDTO>>
    searchEmployees(
            @RequestParam String keyword
    ) {

        return ResponseEntity.ok(
                employeeService
                        .searchEmployees(
                                keyword
                        )
        );
    }

    // =========================================================
    // DEPARTMENT
    // =========================================================

    @GetMapping("/department/{department}")
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'HR', 'MANAGER')"
    )
    public ResponseEntity<List<EmployeeDTO>>
    getEmployeesByDepartment(
            @PathVariable String department
    ) {

        return ResponseEntity.ok(
                employeeService
                        .getEmployeesByDepartment(
                                department
                        )
        );
    }

    // =========================================================
    // STATUS
    // =========================================================

    @GetMapping("/status/{status}")
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'HR', 'MANAGER')"
    )
    public ResponseEntity<List<EmployeeDTO>>
    getEmployeesByStatus(
            @PathVariable String status
    ) {

        return ResponseEntity.ok(
                employeeService
                        .getEmployeesByStatus(
                                status
                        )
        );
    }
}