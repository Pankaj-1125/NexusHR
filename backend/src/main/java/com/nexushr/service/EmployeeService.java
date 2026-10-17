package com.nexushr.service;

import com.nexushr.dto.EmployeeDTO;
import com.nexushr.entity.Employee;
import com.nexushr.entity.User;
import com.nexushr.repository.EmployeeRepository;
import com.nexushr.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final AuditLogService auditLogService;


    /*
     * Runtime SQL bridge used for permanent deletion.
     *
     * The database migration owns the ON DELETE CASCADE rules.
     *
     * Explicit SQL ordering avoids persistence-context problems
     * during permanent employee deletion.
     */
    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;


    public EmployeeService(
            EmployeeRepository employeeRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuditLogService auditLogService
    ) {

        this.employeeRepository =
                employeeRepository;

        this.userRepository =
                userRepository;

        this.passwordEncoder =
                passwordEncoder;

        this.auditLogService =
                auditLogService;
    }


    // =========================================================
    // CREATE EMPLOYEE
    // =========================================================

    @Transactional
    public EmployeeDTO addEmployee(
            EmployeeDTO dto
    ) {

        if (
                dto.getEmail() == null
                        ||
                        dto.getEmail().isBlank()
        ) {

            throw new RuntimeException(
                    "Employee email is required"
            );
        }


        String normalizedEmail =
                dto.getEmail()
                        .trim()
                        .toLowerCase();


        if (
                employeeRepository
                        .findByEmail(
                                normalizedEmail
                        )
                        .isPresent()
        ) {

            throw new RuntimeException(
                    "Employee with email "
                            + normalizedEmail
                            + " already exists"
            );
        }


        Employee employee =
                new Employee();


        employee.setFirstName(
                dto.getFirstName()
        );


        employee.setLastName(
                dto.getLastName()
        );


        employee.setEmail(
                normalizedEmail
        );


        employee.setPhone(
                dto.getPhone()
        );


        employee.setDepartment(
                dto.getDepartment()
        );


        employee.setJobTitle(
                dto.getJobTitle()
        );


        employee.setJoiningDate(
                dto.getJoiningDate()
        );


        employee.setSalary(
                dto.getSalary()
        );


        employee.setEmploymentStatus(
                dto.getEmploymentStatus() != null
                        &&
                        !dto.getEmploymentStatus().isBlank()

                        ? dto.getEmploymentStatus()
                        .trim()
                        .toUpperCase()

                        : "ACTIVE"
        );


        employee.setApprovalStatus(
                "PENDING"
        );


        // =====================================================
        // CREATE LINKED LOGIN ACCOUNT
        // =====================================================

        User user =
                new User();


        /*
         * Internal pending username.
         *
         * Employee/Manager/HR will choose their username
         * during first-time password setup.
         */
        user.setUsername(
                generatePendingUsername()
        );


        /*
         * Internal bootstrap password.
         *
         * This password is never given to the user.
         *
         * The user creates their actual password during
         * first-time account setup / password reset.
         */
        user.setPassword(
                passwordEncoder.encode(
                        UUID.randomUUID()
                                .toString()
                                + "Aa1"
                )
        );


        /*
         * IMPORTANT:
         *
         * Previously every user was always created as:
         *
         * EMPLOYEE
         *
         * Now the role supplied by Admin is respected.
         *
         * Allowed:
         *
         * EMPLOYEE
         * MANAGER
         * HR
         *
         * ADMIN is intentionally not allowed here.
         */
        user.setRole(
                normalizeRequestedRole(
                        dto.getRole()
                )
        );


        user.setEnabled(
                true
        );


        user.setRecoveryEmail(
                normalizedEmail
        );


        User savedUser =
                userRepository.save(
                        user
                );


        employee.setUser(
                savedUser
        );


        Employee saved =
                employeeRepository.save(
                        employee
                );


        auditLogService.log(
                "EMPLOYEE_CREATED",
                "EMPLOYEE",
                saved.getId(),
                "Employee created: "
                        + saved.getFirstName()
                        + " "
                        + saved.getLastName()
                        + " | Role: "
                        + savedUser.getRole()
        );


        return convertToDTO(
                saved
        );
    }


    // =========================================================
    // MANUAL ONBOARDING
    //
    // Controller currently blocks this endpoint.
    //
    // Kept for backward compatibility / internal use.
    // =========================================================

    @Transactional
    public EmployeeDTO onboardEmployee(
            Long employeeId,
            String username,
            String temporaryPassword
    ) {

        Employee employee =
                employeeRepository
                        .findById(
                                employeeId
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Employee not found with ID: "
                                                + employeeId
                                )
                        );


        if (
                employee.getUser()
                        != null
        ) {

            throw new RuntimeException(
                    "Employee already has a login account"
            );
        }


        if (
                username == null
                        ||
                        username.trim().isEmpty()
        ) {

            throw new RuntimeException(
                    "Username is required"
            );
        }


        if (
                temporaryPassword == null
                        ||
                        temporaryPassword.isBlank()
        ) {

            throw new RuntimeException(
                    "Temporary password is required"
            );
        }


        String normalizedUsername =
                username.trim();


        if (
                userRepository
                        .findByUsername(
                                normalizedUsername
                        )
                        .isPresent()
        ) {

            throw new RuntimeException(
                    "Username "
                            + normalizedUsername
                            + " already exists"
            );
        }


        User user =
                new User();


        user.setUsername(
                normalizedUsername
        );


        user.setPassword(
                passwordEncoder.encode(
                        temporaryPassword
                )
        );


        /*
         * This method is legacy/manual onboarding.
         *
         * Since the active controller has disabled manual
         * onboarding, keep EMPLOYEE here for compatibility.
         */
        user.setRole(
                "EMPLOYEE"
        );


        user.setEnabled(
                true
        );


        if (
                employee.getEmail()
                        != null
        ) {

            user.setRecoveryEmail(
                    employee.getEmail()
            );
        }


        User savedUser =
                userRepository.save(
                        user
                );


        employee.setUser(
                savedUser
        );


        employee.setEmploymentStatus(
                "ACTIVE"
        );


        Employee savedEmployee =
                employeeRepository.save(
                        employee
                );


        auditLogService.log(
                "EMPLOYEE_ONBOARDED",
                "EMPLOYEE",
                savedEmployee.getId(),
                "Employee onboarded with login username: "
                        + normalizedUsername
        );


        return convertToDTO(
                savedEmployee
        );
    }


    // =========================================================
    // APPROVE EMPLOYEE
    // =========================================================

    @Transactional
    public EmployeeDTO approveEmployee(
            Long employeeId
    ) {

        Employee employee =
                getEmployeeEntity(
                        employeeId
                );


        if (
                "APPROVED".equalsIgnoreCase(
                        employee.getApprovalStatus()
                )
        ) {

            throw new RuntimeException(
                    "Employee is already approved"
            );
        }


        if (
                "REJECTED".equalsIgnoreCase(
                        employee.getApprovalStatus()
                )
        ) {

            throw new RuntimeException(
                    "Rejected employee cannot be approved directly"
            );
        }


        employee.setApprovalStatus(
                "APPROVED"
        );


        Employee saved =
                employeeRepository.save(
                        employee
                );


        auditLogService.log(
                "EMPLOYEE_APPROVED",
                "EMPLOYEE",
                saved.getId(),
                "Employee approval completed"
        );


        return convertToDTO(
                saved
        );
    }


    // =========================================================
    // REJECT EMPLOYEE
    // =========================================================

    @Transactional
    public EmployeeDTO rejectEmployee(
            Long employeeId
    ) {

        Employee employee =
                getEmployeeEntity(
                        employeeId
                );


        if (
                "REJECTED".equalsIgnoreCase(
                        employee.getApprovalStatus()
                )
        ) {

            throw new RuntimeException(
                    "Employee is already rejected"
            );
        }


        if (
                "APPROVED".equalsIgnoreCase(
                        employee.getApprovalStatus()
                )
        ) {

            throw new RuntimeException(
                    "Approved employee cannot be rejected"
            );
        }


        employee.setApprovalStatus(
                "REJECTED"
        );


        Employee saved =
                employeeRepository.save(
                        employee
                );


        auditLogService.log(
                "EMPLOYEE_REJECTED",
                "EMPLOYEE",
                saved.getId(),
                "Employee approval rejected"
        );


        return convertToDTO(
                saved
        );
    }


    // =========================================================
    // OFFBOARD EMPLOYEE
    // =========================================================

    @Transactional
    public EmployeeDTO offboardEmployee(
            Long employeeId
    ) {

        Employee employee =
                getEmployeeEntity(
                        employeeId
                );


        employee.setEmploymentStatus(
                "INACTIVE"
        );


        if (
                employee.getUser()
                        != null
        ) {

            employee.getUser()
                    .setEnabled(
                            false
                    );


            userRepository.save(
                    employee.getUser()
            );
        }


        Employee saved =
                employeeRepository.save(
                        employee
                );


        auditLogService.log(
                "EMPLOYEE_OFFBOARDED",
                "EMPLOYEE",
                saved.getId(),
                "Employee offboarded"
        );


        return convertToDTO(
                saved
        );
    }


    // =========================================================
    // REACTIVATE EMPLOYEE
    // =========================================================

    @Transactional
    public EmployeeDTO reactivateEmployee(
            Long employeeId
    ) {

        Employee employee =
                getEmployeeEntity(
                        employeeId
                );


        employee.setEmploymentStatus(
                "ACTIVE"
        );


        if (
                employee.getUser()
                        != null
        ) {

            employee.getUser()
                    .setEnabled(
                            true
                    );


            userRepository.save(
                    employee.getUser()
            );
        }


        Employee saved =
                employeeRepository.save(
                        employee
                );


        auditLogService.log(
                "EMPLOYEE_REACTIVATED",
                "EMPLOYEE",
                saved.getId(),
                "Employee reactivated"
        );


        return convertToDTO(
                saved
        );
    }


    // =========================================================
    // GET ALL EMPLOYEES
    // =========================================================

    public List<EmployeeDTO> getAllEmployees() {

        return employeeRepository
                .findAll()
                .stream()
                .map(
                        this::convertToDTO
                )
                .collect(
                        Collectors.toList()
                );
    }


    // =========================================================
    // GET EMPLOYEE BY ID
    // =========================================================

    public EmployeeDTO getEmployeeById(
            Long id
    ) {

        return convertToDTO(
                getEmployeeEntity(
                        id
                )
        );
    }


    // =========================================================
    // GET EMPLOYEE BY EMAIL
    // =========================================================

    public EmployeeDTO getEmployeeByEmail(
            String email
    ) {

        if (
                email == null
                        ||
                        email.isBlank()
        ) {

            throw new RuntimeException(
                    "Employee email is required"
            );
        }


        String normalizedEmail =
                email
                        .trim()
                        .toLowerCase();


        Employee employee =
                employeeRepository
                        .findByEmail(
                                normalizedEmail
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Employee not found with email: "
                                                + normalizedEmail
                                )
                        );


        return convertToDTO(
                employee
        );
    }


    // =========================================================
    // UPDATE EMPLOYEE
    // =========================================================

    @Transactional
    public EmployeeDTO updateEmployee(
            Long id,
            EmployeeDTO dto
    ) {

        Employee employee =
                getEmployeeEntity(
                        id
                );


        // =====================================================
        // EMAIL
        // =====================================================

        if (
                dto.getEmail() != null
                        &&
                        !dto.getEmail().isBlank()
                        &&
                        !dto.getEmail()
                                .equalsIgnoreCase(
                                        employee.getEmail()
                                )
        ) {

            String normalizedEmail =
                    dto.getEmail()
                            .trim()
                            .toLowerCase();


            employeeRepository
                    .findByEmail(
                            normalizedEmail
                    )
                    .ifPresent(existing -> {

                        if (
                                !existing.getId()
                                        .equals(
                                                employee.getId()
                                        )
                        ) {

                            throw new RuntimeException(
                                    "Another employee already uses email: "
                                            + normalizedEmail
                            );
                        }
                    });


            employee.setEmail(
                    normalizedEmail
            );


            /*
             * Keep login recovery email synchronized.
             */
            if (
                    employee.getUser()
                            != null
            ) {

                employee.getUser()
                        .setRecoveryEmail(
                                normalizedEmail
                        );


                userRepository.save(
                        employee.getUser()
                );
            }
        }


        // =====================================================
        // FIRST NAME
        // =====================================================

        if (
                dto.getFirstName()
                        != null
        ) {

            employee.setFirstName(
                    dto.getFirstName()
            );
        }


        // =====================================================
        // LAST NAME
        // =====================================================

        if (
                dto.getLastName()
                        != null
        ) {

            employee.setLastName(
                    dto.getLastName()
            );
        }


        // =====================================================
        // PHONE
        // =====================================================

        if (
                dto.getPhone()
                        != null
        ) {

            employee.setPhone(
                    dto.getPhone()
            );
        }


        // =====================================================
        // DEPARTMENT
        // =====================================================

        if (
                dto.getDepartment()
                        != null
        ) {

            employee.setDepartment(
                    dto.getDepartment()
            );
        }


        // =====================================================
        // JOB TITLE
        // =====================================================

        if (
                dto.getJobTitle()
                        != null
        ) {

            employee.setJobTitle(
                    dto.getJobTitle()
            );
        }


        // =====================================================
        // JOINING DATE
        // =====================================================

        if (
                dto.getJoiningDate()
                        != null
        ) {

            employee.setJoiningDate(
                    dto.getJoiningDate()
            );
        }


        // =====================================================
        // SALARY
        // =====================================================

        if (
                dto.getSalary()
                        != null
        ) {

            employee.setSalary(
                    dto.getSalary()
            );
        }


        // =====================================================
        // EMPLOYMENT STATUS
        // =====================================================

        if (
                dto.getEmploymentStatus()
                        != null
        ) {

            String status =
                    dto.getEmploymentStatus()
                            .trim()
                            .toUpperCase();


            employee.setEmploymentStatus(
                    status
            );


            if (
                    employee.getUser()
                            != null
            ) {

                employee.getUser()
                        .setEnabled(
                                "ACTIVE".equals(
                                        status
                                )
                        );


                userRepository.save(
                        employee.getUser()
                );
            }
        }


        /*
         * IMPORTANT SECURITY DECISION:
         *
         * Normal employee edit does NOT change account role.
         *
         * Otherwise MANAGER users, who can edit employees,
         * could potentially submit:
         *
         * role = ADMIN
         *
         * or elevate another account.
         *
         * Role is therefore assigned only during creation
         * through Admin/HR protected employee creation.
         */


        Employee updated =
                employeeRepository.save(
                        employee
                );


        auditLogService.log(
                "EMPLOYEE_UPDATED",
                "EMPLOYEE",
                updated.getId(),
                "Employee profile updated"
        );


        return convertToDTO(
                updated
        );
    }


    // =========================================================
    // PERMANENT DELETE EMPLOYEE
    // =========================================================

    @Transactional
    public void deleteEmployee(
            Long id
    ) {

        Employee employee =
                employeeRepository
                        .findById(
                                id
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Employee not found with ID: "
                                                + id
                                )
                        );


        User linkedUser =
                employee.getUser();


        Long employeeId =
                employee.getId();


        String employeeName =
                (
                        safe(
                                employee.getFirstName()
                        )
                                + " "
                                + safe(
                                employee.getLastName()
                        )
                ).trim();


        String linkedUsername =
                linkedUser == null

                        ? null

                        : linkedUser
                        .getUsername();


        auditLogService.log(
                "EMPLOYEE_DELETED",
                "EMPLOYEE",
                employeeId,
                "Employee deleted permanently with linked data: "
                        + employeeName
        );


        /*
         * Production / PostgreSQL deletion path.
         */
        if (
                jdbcTemplate
                        != null
        ) {

            // =================================================
            // AUTH TOKENS
            // =================================================

            if (
                    linkedUsername != null
                            &&
                            !linkedUsername.isBlank()
            ) {

                jdbcTemplate.update(
                        "DELETE FROM password_reset_tokens WHERE username = ?",
                        linkedUsername
                );


                jdbcTemplate.update(
                        "DELETE FROM refresh_tokens WHERE username = ?",
                        linkedUsername
                );
            }


            // =================================================
            // NOTIFICATIONS
            // =================================================

            jdbcTemplate.update(
                    "DELETE FROM notifications WHERE employee_id = ?",
                    employeeId
            );


            // =================================================
            // EMPLOYEE
            // =================================================

            int deletedEmployees =
                    jdbcTemplate.update(
                            "DELETE FROM employees WHERE id = ?",
                            employeeId
                    );


            if (
                    deletedEmployees
                            != 1
            ) {

                throw new RuntimeException(
                        "Employee deletion failed for ID: "
                                + employeeId
                );
            }


            // =================================================
            // LINKED USER
            // =================================================

            if (
                    linkedUser != null
                            &&
                            linkedUser.getId()
                                    != null
            ) {

                jdbcTemplate.update(
                        "DELETE FROM users WHERE id = ?",
                        linkedUser.getId()
                );
            }


            return;
        }


        /*
         * Repository fallback.
         *
         * Mainly useful for isolated tests where JdbcTemplate
         * is unavailable.
         */

        employeeRepository.delete(
                employee
        );


        employeeRepository.flush();


        if (
                linkedUser
                        != null
        ) {

            userRepository.delete(
                    linkedUser
            );


            userRepository.flush();
        }
    }


    // =========================================================
    // SEARCH EMPLOYEES
    // =========================================================

    public List<EmployeeDTO> searchEmployees(
            String keyword
    ) {

        String search =
                keyword == null

                        ? ""

                        : keyword
                        .toLowerCase()
                        .trim();


        return employeeRepository
                .findAll()
                .stream()
                .filter(employee ->

                        containsIgnoreCase(
                                employee.getFirstName(),
                                search
                        )

                                ||

                                containsIgnoreCase(
                                        employee.getLastName(),
                                        search
                                )

                                ||

                                containsIgnoreCase(
                                        employee.getEmail(),
                                        search
                                )

                                ||

                                containsIgnoreCase(
                                        employee.getDepartment(),
                                        search
                                )

                                ||

                                containsIgnoreCase(
                                        employee.getJobTitle(),
                                        search
                                )
                )
                .map(
                        this::convertToDTO
                )
                .collect(
                        Collectors.toList()
                );
    }


    // =========================================================
    // EMPLOYEES BY DEPARTMENT
    // =========================================================

    public List<EmployeeDTO> getEmployeesByDepartment(
            String department
    ) {

        return employeeRepository
                .findAll()
                .stream()
                .filter(employee ->

                        employee.getDepartment()
                                != null

                                &&

                                employee.getDepartment()
                                        .equalsIgnoreCase(
                                                department
                                        )
                )
                .map(
                        this::convertToDTO
                )
                .collect(
                        Collectors.toList()
                );
    }


    // =========================================================
    // EMPLOYEES BY STATUS
    // =========================================================

    public List<EmployeeDTO> getEmployeesByStatus(
            String status
    ) {

        return employeeRepository
                .findAll()
                .stream()
                .filter(employee ->

                        employee.getEmploymentStatus()
                                != null

                                &&

                                employee.getEmploymentStatus()
                                        .equalsIgnoreCase(
                                                status
                                        )
                )
                .map(
                        this::convertToDTO
                )
                .collect(
                        Collectors.toList()
                );
    }


    // =========================================================
    // GENERATE INTERNAL PENDING USERNAME
    // =========================================================

    private String generatePendingUsername() {

        String candidate;


        do {

            candidate =
                    "pending_"
                            + UUID.randomUUID()
                            .toString()
                            .replace(
                                    "-",
                                    ""
                            )
                            .substring(
                                    0,
                                    12
                            );

        } while (

                userRepository
                        .findByUsername(
                                candidate
                        )
                        .isPresent()
        );


        return candidate;
    }


    // =========================================================
    // NORMALIZE ACCOUNT ROLE
    // =========================================================

    private String normalizeRequestedRole(
            String requestedRole
    ) {

        /*
         * Backward compatibility:
         *
         * If frontend does not send role,
         * create ordinary EMPLOYEE.
         */

        if (
                requestedRole == null
                        ||
                        requestedRole.isBlank()
        ) {

            return "EMPLOYEE";
        }


        String role =
                requestedRole
                        .trim()
                        .toUpperCase();


        /*
         * ADMIN is deliberately excluded.
         *
         * Normal employee creation must never become
         * an unrestricted Admin account creation endpoint.
         */

        return switch (role) {

            case "EMPLOYEE",
                 "MANAGER",
                 "HR" -> role;


            default ->
                    throw new RuntimeException(
                            "Invalid employee role: "
                                    + requestedRole
                    );
        };
    }


    // =========================================================
    // INTERNAL ENTITY HELPER
    // =========================================================

    private Employee getEmployeeEntity(
            Long id
    ) {

        return employeeRepository
                .findById(
                        id
                )
                .orElseThrow(() ->
                        new RuntimeException(
                                "Employee not found with ID: "
                                        + id
                        )
                );
    }


    // =========================================================
    // CONVERT ENTITY TO DTO
    // =========================================================

    private EmployeeDTO convertToDTO(
            Employee employee
    ) {

        EmployeeDTO dto =
                new EmployeeDTO();


        dto.setId(
                employee.getId()
        );


        dto.setFirstName(
                employee.getFirstName()
        );


        dto.setLastName(
                employee.getLastName()
        );


        dto.setEmail(
                employee.getEmail()
        );


        dto.setPhone(
                employee.getPhone()
        );


        dto.setDepartment(
                employee.getDepartment()
        );


        dto.setJobTitle(
                employee.getJobTitle()
        );


        dto.setJoiningDate(
                employee.getJoiningDate()
        );


        dto.setSalary(
                employee.getSalary()
        );


        dto.setEmploymentStatus(
                employee.getEmploymentStatus()
        );


        dto.setApprovalStatus(
                employee.getApprovalStatus()
        );


        if (
                employee.getUser()
                        != null
        ) {

            dto.setUserId(
                    employee.getUser()
                            .getId()
            );


            /*
             * NEW:
             * Return linked account role to frontend.
             */
            dto.setRole(
                    employee.getUser()
                            .getRole()
            );


            String linkedUsername =
                    employee.getUser()
                            .getUsername();


            /*
             * Never expose internal pending usernames.
             */
            if (
                    linkedUsername != null
                            &&
                            !linkedUsername.startsWith(
                                    "pending_"
                            )
            ) {

                dto.setUsername(
                        linkedUsername
                );
            }
        }


        return dto;
    }


    // =========================================================
    // STRING SEARCH HELPER
    // =========================================================

    private boolean containsIgnoreCase(
            String value,
            String search
    ) {

        return value != null
                &&
                value
                        .toLowerCase()
                        .contains(
                                search
                        );
    }


    // =========================================================
    // NULL-SAFE STRING
    // =========================================================

    private String safe(
            String value
    ) {

        return value == null
                ? ""
                : value;
    }
}