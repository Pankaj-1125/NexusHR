package com.nexushr.controller;

import com.nexushr.entity.RefreshToken;
import com.nexushr.security.JwtService;
import com.nexushr.repository.EmployeeRepository;
import com.nexushr.service.AuditLogService;
import com.nexushr.service.RefreshTokenService;
import com.nexushr.service.PasswordResetService;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private PasswordResetService passwordResetService;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private HttpServletRequest httpRequest;

    @Mock
    private Authentication authentication;

    private AuthController authController;

    private UserDetails userDetails;

    @BeforeEach
    void setUp() {

        authController = new AuthController(
                authenticationManager,
                jwtService,
                refreshTokenService,
                userDetailsService,
                auditLogService,
                passwordResetService,
                employeeRepository
        );

        userDetails = User.withUsername("admin")
                .password("password")
                .authorities(
                        new SimpleGrantedAuthority("ROLE_ADMIN")
                )
                .build();

        /*
         * Some negative refresh-token tests return before the
         * controller reads the request IP. Lenient prevents Mockito
         * from treating this shared setup as unnecessary stubbing.
         */
        lenient()
                .when(httpRequest.getRemoteAddr())
                .thenReturn("127.0.0.1");
    }

    // -------------------------------------------------------------------------
    // LOGIN
    // -------------------------------------------------------------------------

    @Test
    void loginShouldReturnAccessAndRefreshTokens() {

        when(authenticationManager.authenticate(
                any(UsernamePasswordAuthenticationToken.class)
        )).thenReturn(authentication);

        when(authentication.getPrincipal())
                .thenReturn(userDetails);

        when(authentication.getAuthorities())
                .thenAnswer(invocation ->
                        List.of(
                                new SimpleGrantedAuthority("ROLE_ADMIN")
                        )
                );

        when(jwtService.generateToken(userDetails))
                .thenReturn("access-token");

        when(refreshTokenService.createRefreshToken(userDetails))
                .thenReturn("refresh-token");

        AuthController.LoginRequest request =
                new AuthController.LoginRequest(
                        "admin",
                        "password"
                );

        ResponseEntity<Map<String, Object>> response =
                authController.login(
                        request,
                        httpRequest
                );

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode()
        );

        Map<String, Object> body =
                response.getBody();

        assertNotNull(body);

        assertEquals(
                "Login successful",
                body.get("message")
        );

        assertEquals(
                "admin",
                body.get("username")
        );

        assertEquals(
                "ROLE_ADMIN",
                body.get("role")
        );

        assertEquals(
                "access-token",
                body.get("token")
        );

        assertEquals(
                "refresh-token",
                body.get("refreshToken")
        );

        verify(authenticationManager)
                .authenticate(
                        any(UsernamePasswordAuthenticationToken.class)
                );

        verify(jwtService)
                .generateToken(userDetails);

        verify(refreshTokenService)
                .createRefreshToken(userDetails);

        verify(auditLogService)
                .log(
                        eq("admin"),
                        eq("LOGIN_SUCCESS"),
                        eq("AUTHENTICATION"),
                        isNull(),
                        eq("User login successful"),
                        eq("127.0.0.1")
                );
    }

    // -------------------------------------------------------------------------
    // REFRESH TOKEN
    // -------------------------------------------------------------------------

    @Test
    void refreshShouldReturnNewAccessAndRefreshTokens() {

        String oldRefreshToken =
                "old-refresh-token";

        RefreshToken storedToken =
                new RefreshToken(
                        "hashed-token",
                        "admin",
                        Instant.now().plusSeconds(3600)
                );

        when(refreshTokenService.validateRefreshToken(
                oldRefreshToken
        )).thenReturn(storedToken);

        when(userDetailsService.loadUserByUsername("admin"))
                .thenReturn(userDetails);

        when(jwtService.generateToken(userDetails))
                .thenReturn("new-access-token");

        when(refreshTokenService.createRefreshToken(userDetails))
                .thenReturn("new-refresh-token");

        AuthController.RefreshRequest request =
                new AuthController.RefreshRequest(
                        oldRefreshToken
                );

        ResponseEntity<Map<String, Object>> response =
                authController.refresh(
                        request,
                        httpRequest
                );

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode()
        );

        Map<String, Object> body =
                response.getBody();

        assertNotNull(body);

        assertEquals(
                "Token refreshed successfully",
                body.get("message")
        );

        assertEquals(
                "admin",
                body.get("username")
        );

        assertEquals(
                "new-access-token",
                body.get("token")
        );

        assertEquals(
                "new-refresh-token",
                body.get("refreshToken")
        );

        verify(refreshTokenService)
                .validateRefreshToken(oldRefreshToken);

        verify(userDetailsService)
                .loadUserByUsername("admin");

        verify(jwtService)
                .generateToken(userDetails);

        verify(refreshTokenService)
                .createRefreshToken(userDetails);

        verify(refreshTokenService)
                .revokeToken(oldRefreshToken);

        verify(auditLogService)
                .log(
                        eq("admin"),
                        eq("TOKEN_REFRESHED"),
                        eq("AUTHENTICATION"),
                        isNull(),
                        eq("Access and refresh tokens renewed"),
                        eq("127.0.0.1")
                );
    }

    @Test
    void refreshShouldReturnUnauthorizedForInvalidToken() {

        String invalidToken =
                "invalid-token";

        when(refreshTokenService.validateRefreshToken(
                invalidToken
        )).thenThrow(
                new IllegalArgumentException(
                        "Invalid refresh token."
                )
        );

        AuthController.RefreshRequest request =
                new AuthController.RefreshRequest(
                        invalidToken
                );

        ResponseEntity<Map<String, Object>> response =
                authController.refresh(
                        request,
                        httpRequest
                );

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                response.getStatusCode()
        );

        Map<String, Object> body =
                response.getBody();

        assertNotNull(body);

        assertEquals(
                "Invalid refresh token.",
                body.get("message")
        );

        verify(refreshTokenService)
                .validateRefreshToken(invalidToken);

        verifyNoInteractions(
                userDetailsService,
                jwtService,
                auditLogService
        );

        verify(refreshTokenService, never())
                .createRefreshToken(any(UserDetails.class));

        verify(refreshTokenService, never())
                .revokeToken(anyString());
    }

    @Test
    void refreshShouldReturnUnauthorizedForExpiredToken() {

        String expiredToken =
                "expired-token";

        when(refreshTokenService.validateRefreshToken(
                expiredToken
        )).thenThrow(
                new IllegalArgumentException(
                        "Refresh token has expired."
                )
        );

        AuthController.RefreshRequest request =
                new AuthController.RefreshRequest(
                        expiredToken
                );

        ResponseEntity<Map<String, Object>> response =
                authController.refresh(
                        request,
                        httpRequest
                );

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                response.getStatusCode()
        );

        assertEquals(
                "Refresh token has expired.",
                response.getBody().get("message")
        );

        verify(refreshTokenService)
                .validateRefreshToken(expiredToken);

        verifyNoInteractions(auditLogService);
    }

    @Test
    void refreshShouldReturnUnauthorizedForRevokedToken() {

        String revokedToken =
                "revoked-token";

        when(refreshTokenService.validateRefreshToken(
                revokedToken
        )).thenThrow(
                new IllegalArgumentException(
                        "Refresh token has been revoked."
                )
        );

        AuthController.RefreshRequest request =
                new AuthController.RefreshRequest(
                        revokedToken
                );

        ResponseEntity<Map<String, Object>> response =
                authController.refresh(
                        request,
                        httpRequest
                );

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                response.getStatusCode()
        );

        assertEquals(
                "Refresh token has been revoked.",
                response.getBody().get("message")
        );

        verify(refreshTokenService)
                .validateRefreshToken(revokedToken);

        verifyNoInteractions(auditLogService);
    }

    // -------------------------------------------------------------------------
    // LOGOUT
    // -------------------------------------------------------------------------

    @Test
    void logoutShouldRevokeRefreshToken() {

        String refreshToken =
                "logout-token";

        AuthController.LogoutRequest request =
                new AuthController.LogoutRequest(
                        refreshToken
                );

        ResponseEntity<Map<String, Object>> response =
                authController.logout(
                        request,
                        httpRequest
                );

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode()
        );

        Map<String, Object> body =
                response.getBody();

        assertNotNull(body);

        assertEquals(
                "Logout successful",
                body.get("message")
        );

        verify(refreshTokenService)
                .revokeToken(refreshToken);

        verify(auditLogService)
                .log(
                        eq("LOGOUT"),
                        eq("AUTHENTICATION"),
                        isNull(),
                        eq("User logout requested"),
                        eq("127.0.0.1")
                );
    }

    @Test
    void logoutShouldBeSuccessfulEvenWhenTokenIsMissing() {

        AuthController.LogoutRequest request =
                new AuthController.LogoutRequest(null);

        ResponseEntity<Map<String, Object>> response =
                authController.logout(
                        request,
                        httpRequest
                );

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode()
        );

        assertEquals(
                "Logout successful",
                response.getBody().get("message")
        );

        verify(refreshTokenService)
                .revokeToken(null);

        verify(auditLogService)
                .log(
                        eq("LOGOUT"),
                        eq("AUTHENTICATION"),
                        isNull(),
                        eq("User logout requested"),
                        eq("127.0.0.1")
                );
    }
}