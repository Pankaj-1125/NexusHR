package com.nexushr.service;

import com.nexushr.entity.RefreshToken;
import com.nexushr.entity.User;
import com.nexushr.repository.RefreshTokenRepository;
import com.nexushr.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private UserDetails userDetails;

    @BeforeEach
    void setUp() {

        ReflectionTestUtils.setField(
                refreshTokenService,
                "refreshTokenExpiration",
                604800000L
        );

        userDetails =
                org.springframework.security.core.userdetails.User
                        .withUsername("admin")
                        .password("password")
                        .roles("ADMIN")
                        .build();
    }

    @Test
    void createRefreshTokenShouldGenerateAndStoreToken() {

        when(refreshTokenRepository
                .findByUsernameAndRevokedFalse("admin"))
                .thenReturn(List.of());

        ArgumentCaptor<RefreshToken> captor =
                ArgumentCaptor.forClass(RefreshToken.class);

        String token =
                refreshTokenService
                        .createRefreshToken(userDetails);

        assertNotNull(token);
        assertFalse(token.isBlank());

        verify(refreshTokenRepository)
                .save(captor.capture());

        RefreshToken savedToken =
                captor.getValue();

        assertEquals(
                "admin",
                savedToken.getUsername()
        );

        assertNotNull(
                savedToken.getTokenHash()
        );

        assertEquals(
                64,
                savedToken.getTokenHash().length()
        );

        assertNotNull(
                savedToken.getExpiresAt()
        );

        assertFalse(
                savedToken.isRevoked()
        );

        assertNotEquals(
                token,
                savedToken.getTokenHash()
        );
    }

    @Test
    void createRefreshTokenShouldRevokeExistingTokensFirst() {

        RefreshToken existingToken =
                new RefreshToken(
                        "existing-hash",
                        "admin",
                        Instant.now()
                                .plusSeconds(3600)
                );

        when(refreshTokenRepository
                .findByUsernameAndRevokedFalse("admin"))
                .thenReturn(
                        List.of(existingToken)
                );

        String token =
                refreshTokenService
                        .createRefreshToken(userDetails);

        assertNotNull(token);

        assertTrue(
                existingToken.isRevoked()
        );

        verify(refreshTokenRepository)
                .saveAll(
                        List.of(existingToken)
                );

        verify(refreshTokenRepository)
                .save(any(RefreshToken.class));
    }

    @Test
    void createRefreshTokenShouldRejectNullUserDetails() {

        assertThrows(
                IllegalArgumentException.class,
                () -> refreshTokenService
                        .createRefreshToken(null)
        );

        verifyNoInteractions(
                refreshTokenRepository
        );

        verifyNoInteractions(
                userRepository
        );
    }

    @Test
    void createRefreshTokenShouldRejectDisabledUser() {

        UserDetails disabledUser =
                org.springframework.security.core.userdetails.User
                        .withUsername("disabled")
                        .password("password")
                        .roles("EMPLOYEE")
                        .disabled(true)
                        .build();

        assertFalse(
                disabledUser.isEnabled()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> refreshTokenService
                        .createRefreshToken(
                                disabledUser
                        )
        );

        verifyNoInteractions(
                refreshTokenRepository
        );

        verifyNoInteractions(
                userRepository
        );
    }

    @Test
    void hashTokenShouldReturnConsistentSha256Hash() {

        String token =
                "test-refresh-token";

        String hash1 =
                refreshTokenService
                        .hashToken(token);

        String hash2 =
                refreshTokenService
                        .hashToken(token);

        assertNotNull(hash1);

        assertEquals(
                64,
                hash1.length()
        );

        assertEquals(
                hash1,
                hash2
        );
    }

    @Test
    void validateRefreshTokenShouldReturnValidToken() {

        String rawToken =
                "valid-refresh-token";

        RefreshToken refreshToken =
                new RefreshToken(
                        refreshTokenService
                                .hashToken(rawToken),
                        "admin",
                        Instant.now()
                                .plusSeconds(3600)
                );

        User user =
                new User(
                        "admin",
                        "password",
                        "ADMIN",
                        true
                );

        when(refreshTokenRepository
                .findByTokenHash(
                        refreshToken.getTokenHash()
                ))
                .thenReturn(
                        Optional.of(refreshToken)
                );

        when(userRepository
                .findByUsername("admin"))
                .thenReturn(
                        Optional.of(user)
                );

        RefreshToken result =
                refreshTokenService
                        .validateRefreshToken(
                                rawToken
                        );

        assertSame(
                refreshToken,
                result
        );

        assertFalse(
                result.isRevoked()
        );
    }

    @Test
    void validateRefreshTokenShouldRejectMissingToken() {

        assertThrows(
                IllegalArgumentException.class,
                () -> refreshTokenService
                        .validateRefreshToken(null)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> refreshTokenService
                        .validateRefreshToken("")
        );

        verifyNoInteractions(
                refreshTokenRepository
        );

        verifyNoInteractions(
                userRepository
        );
    }

    @Test
    void validateRefreshTokenShouldRejectInvalidToken() {

        when(refreshTokenRepository
                .findByTokenHash(
                        any(String.class)
                ))
                .thenReturn(
                        Optional.empty()
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> refreshTokenService
                                .validateRefreshToken(
                                        "invalid-token"
                                )
                );

        assertEquals(
                "Invalid refresh token.",
                exception.getMessage()
        );

        verifyNoInteractions(
                userRepository
        );
    }

    @Test
    void validateRefreshTokenShouldRejectRevokedToken() {

        String rawToken =
                "revoked-token";

        RefreshToken refreshToken =
                new RefreshToken(
                        refreshTokenService
                                .hashToken(rawToken),
                        "admin",
                        Instant.now()
                                .plusSeconds(3600)
                );

        refreshToken.setRevoked(true);

        when(refreshTokenRepository
                .findByTokenHash(
                        refreshToken.getTokenHash()
                ))
                .thenReturn(
                        Optional.of(refreshToken)
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> refreshTokenService
                                .validateRefreshToken(
                                        rawToken
                                )
                );

        assertEquals(
                "Refresh token has been revoked.",
                exception.getMessage()
        );

        verifyNoInteractions(
                userRepository
        );
    }

    @Test
    void validateRefreshTokenShouldRejectExpiredToken() {

        String rawToken =
                "expired-token";

        RefreshToken refreshToken =
                new RefreshToken(
                        refreshTokenService
                                .hashToken(rawToken),
                        "admin",
                        Instant.now()
                                .minusSeconds(60)
                );

        when(refreshTokenRepository
                .findByTokenHash(
                        refreshToken.getTokenHash()
                ))
                .thenReturn(
                        Optional.of(refreshToken)
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> refreshTokenService
                                .validateRefreshToken(
                                        rawToken
                                )
                );

        assertEquals(
                "Refresh token has expired.",
                exception.getMessage()
        );

        verifyNoInteractions(
                userRepository
        );
    }

    @Test
    void validateRefreshTokenShouldRejectMissingUser() {

        String rawToken =
                "missing-user-token";

        RefreshToken refreshToken =
                new RefreshToken(
                        refreshTokenService
                                .hashToken(rawToken),
                        "missing-user",
                        Instant.now()
                                .plusSeconds(3600)
                );

        when(refreshTokenRepository
                .findByTokenHash(
                        refreshToken.getTokenHash()
                ))
                .thenReturn(
                        Optional.of(refreshToken)
                );

        when(userRepository
                .findByUsername("missing-user"))
                .thenReturn(
                        Optional.empty()
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> refreshTokenService
                                .validateRefreshToken(
                                        rawToken
                                )
                );

        assertEquals(
                "User account not found.",
                exception.getMessage()
        );
    }

    @Test
    void validateRefreshTokenShouldRejectDisabledUser() {

        String rawToken =
                "disabled-user-token";

        RefreshToken refreshToken =
                new RefreshToken(
                        refreshTokenService
                                .hashToken(rawToken),
                        "disabled",
                        Instant.now()
                                .plusSeconds(3600)
                );

        User disabledUser =
                new User(
                        "disabled",
                        "password",
                        "EMPLOYEE",
                        false
                );

        when(refreshTokenRepository
                .findByTokenHash(
                        refreshToken.getTokenHash()
                ))
                .thenReturn(
                        Optional.of(refreshToken)
                );

        when(userRepository
                .findByUsername("disabled"))
                .thenReturn(
                        Optional.of(disabledUser)
                );

        assertFalse(
                disabledUser.isEnabled()
        );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> refreshTokenService
                                .validateRefreshToken(
                                        rawToken
                                )
                );

        assertEquals(
                "User account is disabled.",
                exception.getMessage()
        );
    }

    @Test
    void revokeTokenShouldMarkTokenAsRevoked() {

        String rawToken =
                "logout-token";

        RefreshToken refreshToken =
                new RefreshToken(
                        refreshTokenService
                                .hashToken(rawToken),
                        "admin",
                        Instant.now()
                                .plusSeconds(3600)
                );

        when(refreshTokenRepository
                .findByTokenHash(
                        refreshToken.getTokenHash()
                ))
                .thenReturn(
                        Optional.of(refreshToken)
                );

        refreshTokenService.revokeToken(
                rawToken
        );

        assertTrue(
                refreshToken.isRevoked()
        );

        verify(refreshTokenRepository)
                .save(refreshToken);
    }

    @Test
    void revokeTokenShouldDoNothingForMissingToken() {

        refreshTokenService.revokeToken(null);

        refreshTokenService.revokeToken("");

        verifyNoInteractions(
                refreshTokenRepository
        );

        verifyNoInteractions(
                userRepository
        );
    }

    @Test
    void revokeAllForUserShouldRevokeAllActiveTokens() {

        RefreshToken token1 =
                new RefreshToken(
                        "hash-1",
                        "admin",
                        Instant.now()
                                .plusSeconds(3600)
                );

        RefreshToken token2 =
                new RefreshToken(
                        "hash-2",
                        "admin",
                        Instant.now()
                                .plusSeconds(3600)
                );

        List<RefreshToken> tokens =
                List.of(
                        token1,
                        token2
                );

        when(refreshTokenRepository
                .findByUsernameAndRevokedFalse(
                        "admin"
                ))
                .thenReturn(tokens);

        refreshTokenService.revokeAllForUser(
                "admin"
        );

        assertTrue(
                token1.isRevoked()
        );

        assertTrue(
                token2.isRevoked()
        );

        verify(refreshTokenRepository)
                .saveAll(tokens);
    }

    @Test
    void revokeAllForUserShouldDoNothingForBlankUsername() {

        refreshTokenService.revokeAllForUser(null);

        refreshTokenService.revokeAllForUser("");

        verifyNoInteractions(
                refreshTokenRepository
        );

        verifyNoInteractions(
                userRepository
        );
    }
}