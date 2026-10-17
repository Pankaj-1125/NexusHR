package com.nexushr.controller;

import com.nexushr.entity.Employee;
import com.nexushr.entity.RefreshToken;

import com.nexushr.repository.EmployeeRepository;

import com.nexushr.security.JwtService;

import com.nexushr.service.AuditLogService;
import com.nexushr.service.PasswordResetService;
import com.nexushr.service.RefreshTokenService;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;

    private final JwtService jwtService;

    private final RefreshTokenService refreshTokenService;

    private final UserDetailsService userDetailsService;

    private final AuditLogService auditLogService;

    private final EmployeeRepository employeeRepository;

    private final PasswordResetService passwordResetService;


    // =========================================================
    // COOKIE CONFIGURATION
    // =========================================================

    @Value("${nexushr.auth.cookie.secure:false}")
    private boolean secureCookie;

    @Value("${nexushr.auth.cookie.same-site:Lax}")
    private String cookieSameSite;


    public AuthController(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            UserDetailsService userDetailsService,
            AuditLogService auditLogService,
            PasswordResetService passwordResetService,
            EmployeeRepository employeeRepository
    ) {

        this.authenticationManager =
                authenticationManager;

        this.jwtService =
                jwtService;

        this.refreshTokenService =
                refreshTokenService;

        this.userDetailsService =
                userDetailsService;

        this.auditLogService =
                auditLogService;

        this.employeeRepository =
                employeeRepository;

        this.passwordResetService =
                passwordResetService;
    }


    // =========================================================
    // LOGIN
    // USERNAME OR EMPLOYEE EMAIL
    // =========================================================

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {

        String loginIdentifier =
                request.username() == null
                        ? ""
                        : request.username().trim();


        /*
         * Employee can login using:
         *
         * 1. Employee chosen username
         * 2. Employee registered email
         *
         * Admin / Manager / HR continue using username.
         */

        String authenticationUsername =
                employeeRepository
                        .findByEmail(
                                loginIdentifier
                        )
                        .map(
                                Employee::getUser
                        )
                        .filter(
                                user ->
                                        user != null
                        )
                        .map(
                                user ->
                                        user.getUsername()
                        )
                        .orElse(
                                loginIdentifier
                        );


        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                authenticationUsername,
                                request.password()
                        )
                );


        UserDetails userDetails =
                (UserDetails)
                        authentication.getPrincipal();


        String accessToken =
                jwtService.generateToken(
                        userDetails
                );


        String refreshToken =
                refreshTokenService.createRefreshToken(
                        userDetails
                );


        String role =
                authentication
                        .getAuthorities()
                        .stream()
                        .findFirst()
                        .map(
                                authority ->
                                        authority.getAuthority()
                        )
                        .orElse("");


        Optional<Employee> linkedEmployee =
                employeeRepository.findByUserUsername(
                        userDetails.getUsername()
                );


        auditLogService.log(
                userDetails.getUsername(),
                "LOGIN_SUCCESS",
                "AUTHENTICATION",
                null,
                "User login successful",
                getClientIpAddress(
                        httpRequest
                )
        );


        Map<String, Object> response =
                new HashMap<>();


        response.put(
                "message",
                "Login successful"
        );


        response.put(
                "username",
                userDetails.getUsername()
        );


        response.put(
                "role",
                role
        );


        response.put(
                "token",
                accessToken
        );


        response.put(
                "refreshToken",
                refreshToken
        );


        if (linkedEmployee.isPresent()) {

            Employee employee =
                    linkedEmployee.get();


            response.put(
                    "employeeId",
                    employee.getId()
            );


            response.put(
                    "employeeName",
                    buildEmployeeName(
                            employee
                    )
            );


            response.put(
                    "employeeEmail",
                    employee.getEmail()
            );


            response.put(
                    "email",
                    employee.getEmail()
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
                    "salary",
                    employee.getSalary()
            );


            response.put(
                    "employmentStatus",
                    employee.getEmploymentStatus()
            );


            response.put(
                    "approvalStatus",
                    employee.getApprovalStatus()
            );


            response.put(
                    "linked",
                    true
            );

        } else {

            response.put(
                    "employeeId",
                    null
            );


            response.put(
                    "linked",
                    false
            );
        }


        ResponseCookie accessCookie =
                buildAccessCookie(
                        accessToken
                );


        return ResponseEntity
                .ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        accessCookie.toString()
                )
                .body(
                        response
                );
    }


    // =========================================================
    // FORGOT PASSWORD
    // =========================================================

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>>
    forgotPassword(
            @RequestBody
            ForgotPasswordRequest request
    ) {

        /*
         * Always return generic success.
         *
         * Prevents revealing whether a user exists.
         */

        passwordResetService.requestReset(
                request.usernameOrEmail()
        );


        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "If the account exists and has a registered email, a reset code has been sent."
                )
        );
    }


    // =========================================================
    // RESET PASSWORD
    //
    // During first-time EMPLOYEE setup:
    // desiredUsername is required.
    //
    // After first setup:
    // desiredUsername must be null/blank.
    // =========================================================

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>>
    resetPassword(
            @RequestBody
            ResetPasswordRequest request
    ) {

        try {

            passwordResetService.resetPassword(
                    request.username(),
                    request.code(),
                    request.newPassword(),
                    request.desiredUsername()
            );


            return ResponseEntity.ok(
                    Map.of(
                            "message",
                            "Password reset successfully"
                    )
            );

        } catch (RuntimeException exception) {

            Map<String, String> response =
                    new HashMap<>();


            response.put(
                    "message",
                    exception.getMessage()
            );


            return ResponseEntity
                    .badRequest()
                    .body(
                            response
                    );
        }
    }


    // =========================================================
    // CURRENT USER
    // =========================================================

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(
            Authentication authentication
    ) {

        Map<String, Object> response =
                new HashMap<>();


        String username =
                authentication.getName();


        String role =
                authentication
                        .getAuthorities()
                        .stream()
                        .findFirst()
                        .map(
                                authority ->
                                        authority.getAuthority()
                        )
                        .orElse("");


        response.put(
                "username",
                username
        );


        response.put(
                "role",
                role
        );


        Optional<Employee> linkedEmployee =
                employeeRepository.findByUserUsername(
                        username
                );


        if (linkedEmployee.isPresent()) {

            Employee employee =
                    linkedEmployee.get();


            response.put(
                    "employeeId",
                    employee.getId()
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
                    "employeeName",
                    buildEmployeeName(
                            employee
                    )
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
                    "salary",
                    employee.getSalary()
            );


            response.put(
                    "employmentStatus",
                    employee.getEmploymentStatus()
            );


            response.put(
                    "approvalStatus",
                    employee.getApprovalStatus()
            );


            response.put(
                    "linked",
                    true
            );

        } else {

            response.put(
                    "employeeId",
                    null
            );


            response.put(
                    "linked",
                    false
            );
        }


        return ResponseEntity.ok(
                response
        );
    }


    // =========================================================
    // REFRESH TOKEN
    // =========================================================

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(
            @RequestBody RefreshRequest request,
            HttpServletRequest httpRequest
    ) {

        try {

            RefreshToken storedToken =
                    refreshTokenService.validateRefreshToken(
                            request.refreshToken()
                    );


            UserDetails userDetails =
                    userDetailsService.loadUserByUsername(
                            storedToken.getUsername()
                    );


            String newAccessToken =
                    jwtService.generateToken(
                            userDetails
                    );


            String newRefreshToken =
                    refreshTokenService.createRefreshToken(
                            userDetails
                    );


            refreshTokenService.revokeToken(
                    request.refreshToken()
            );


            String role =
                    userDetails
                            .getAuthorities()
                            .stream()
                            .findFirst()
                            .map(
                                    authority ->
                                            authority.getAuthority()
                            )
                            .orElse("");


            Optional<Employee> linkedEmployee =
                    employeeRepository.findByUserUsername(
                            userDetails.getUsername()
                    );


            auditLogService.log(
                    userDetails.getUsername(),
                    "TOKEN_REFRESHED",
                    "AUTHENTICATION",
                    null,
                    "Access and refresh tokens renewed",
                    getClientIpAddress(
                            httpRequest
                    )
            );


            Map<String, Object> response =
                    new HashMap<>();


            /*
             * Required by API contract and existing tests.
             */

            response.put(
                    "message",
                    "Token refreshed successfully"
            );


            response.put(
                    "token",
                    newAccessToken
            );


            response.put(
                    "refreshToken",
                    newRefreshToken
            );


            response.put(
                    "username",
                    userDetails.getUsername()
            );


            response.put(
                    "role",
                    role
            );


            if (linkedEmployee.isPresent()) {

                Employee employee =
                        linkedEmployee.get();


                response.put(
                        "employeeId",
                        employee.getId()
                );


                response.put(
                        "employeeName",
                        buildEmployeeName(
                                employee
                        )
                );


                response.put(
                        "employeeEmail",
                        employee.getEmail()
                );


                response.put(
                        "department",
                        employee.getDepartment()
                );


                response.put(
                        "jobTitle",
                        employee.getJobTitle()
                );

            } else {

                response.put(
                        "employeeId",
                        null
                );
            }


            ResponseCookie accessCookie =
                    buildAccessCookie(
                            newAccessToken
                    );


            return ResponseEntity
                    .ok()
                    .header(
                            HttpHeaders.SET_COOKIE,
                            accessCookie.toString()
                    )
                    .body(
                            response
                    );

        } catch (RuntimeException exception) {

            Map<String, Object> response =
                    new HashMap<>();


            response.put(
                    "message",
                    exception.getMessage()
            );


            return ResponseEntity
                    .status(
                            HttpStatus.UNAUTHORIZED
                    )
                    .body(
                            response
                    );
        }
    }


    // =========================================================
    // LOGOUT
    // =========================================================

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @RequestBody(required = false)
            LogoutRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {

        if (
                request != null
                        &&
                        request.refreshToken() != null
                        &&
                        !request.refreshToken().isBlank()
        ) {

            try {

                refreshTokenService.revokeToken(
                        request.refreshToken()
                );

            } catch (RuntimeException ignored) {

                /*
                 * Logout should remain idempotent.
                 */
            }
        }


        if (
                authentication != null
                        &&
                        authentication.isAuthenticated()
        ) {

            auditLogService.log(
                    authentication.getName(),
                    "LOGOUT",
                    "AUTHENTICATION",
                    null,
                    "User logged out",
                    getClientIpAddress(
                            httpRequest
                    )
            );
        }


        ResponseCookie clearedAccessCookie =
                clearAccessCookie();


        return ResponseEntity
                .ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        clearedAccessCookie.toString()
                )
                .body(
                        Map.of(
                                "message",
                                "Logout successful"
                        )
                );
    }


    // =========================================================
    // BACKWARD-COMPATIBLE LOGOUT
    //
    // Existing tests and older callers use:
    // logout(request, httpRequest)
    // =========================================================

    public ResponseEntity<Map<String, Object>> logout(
            LogoutRequest request,
            HttpServletRequest httpRequest
    ) {

        String refreshToken =
                request == null
                        ? null
                        : request.refreshToken();


        refreshTokenService.revokeToken(
                refreshToken
        );


        auditLogService.log(
                "LOGOUT",
                "AUTHENTICATION",
                null,
                "User logout requested",
                getClientIpAddress(
                        httpRequest
                )
        );


        Map<String, Object> response =
                new HashMap<>();


        response.put(
                "message",
                "Logout successful"
        );


        return ResponseEntity.ok(
                response
        );
    }


    // =========================================================
    // HELPERS
    // =========================================================

    private String buildEmployeeName(
            Employee employee
    ) {

        String firstName =
                employee.getFirstName() == null
                        ? ""
                        : employee
                        .getFirstName()
                        .trim();


        String lastName =
                employee.getLastName() == null
                        ? ""
                        : employee
                        .getLastName()
                        .trim();


        return (
                firstName
                        + " "
                        + lastName
        ).trim();
    }


    // =========================================================
    // ACCESS TOKEN COOKIE
    //
    // LOCAL:
    // secure=false
    // SameSite=Lax
    //
    // RENDER:
    // secure=true
    // SameSite=None
    // =========================================================

    private ResponseCookie buildAccessCookie(
            String accessToken
    ) {

        return ResponseCookie
                .from(
                        "nexushr_access_token",
                        accessToken
                )
                .httpOnly(true)
                .secure(
                        secureCookie
                )
                .sameSite(
                        cookieSameSite
                )
                .path("/")
                .maxAge(
                        24 * 60 * 60
                )
                .build();
    }


    private ResponseCookie clearAccessCookie() {

        return ResponseCookie
                .from(
                        "nexushr_access_token",
                        ""
                )
                .httpOnly(true)
                .secure(
                        secureCookie
                )
                .sameSite(
                        cookieSameSite
                )
                .path("/")
                .maxAge(0)
                .build();
    }


    // =========================================================
    // CLIENT IP ADDRESS
    // =========================================================

    private String getClientIpAddress(
            HttpServletRequest request
    ) {

        if (request == null) {
            return null;
        }


        String forwarded =
                request.getHeader(
                        "X-Forwarded-For"
                );


        if (
                forwarded != null
                        &&
                        !forwarded.isBlank()
        ) {

            return forwarded
                    .split(",")[0]
                    .trim();
        }


        return request.getRemoteAddr();
    }


    // =========================================================
    // REQUEST DTOs
    // =========================================================

    public record LoginRequest(
            String username,
            String password
    ) {
    }


    public record RefreshRequest(
            String refreshToken
    ) {
    }


    public record LogoutRequest(
            String refreshToken
    ) {
    }


    public record ForgotPasswordRequest(
            String usernameOrEmail
    ) {
    }


    public record ResetPasswordRequest(
            String username,
            String code,
            String newPassword,
            String desiredUsername
    ) {

        public ResetPasswordRequest(
                String username,
                String code,
                String newPassword
        ) {

            this(
                    username,
                    code,
                    newPassword,
                    null
            );
        }
    }
}