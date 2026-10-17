package com.nexushr.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private JwtService jwtService;

    private UserDetails userDetails;

    private static final String SECRET =
            "ThisIsAValidNexusHRJwtSecretKeyForTestingOnly123456789";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();

        ReflectionTestUtils.setField(
                jwtService,
                "secret",
                SECRET
        );

        ReflectionTestUtils.setField(
                jwtService,
                "jwtExpiration",
                3600000L
        );

        userDetails = User.withUsername("testuser")
                .password("password")
                .roles("EMPLOYEE")
                .build();
    }

    @Test
    void generateTokenShouldCreateToken() {
        String token = jwtService.generateToken(userDetails);

        assertNotNull(token);
        assertFalse(token.isBlank());
        assertTrue(token.split("\\.").length == 3);
    }

    @Test
    void extractUsernameShouldReturnUsernameFromToken() {
        String token = jwtService.generateToken(userDetails);

        String username = jwtService.extractUsername(token);

        assertEquals("testuser", username);
    }

    @Test
    void extractClaimShouldReturnRequestedClaim() {
        String token = jwtService.generateToken(userDetails);

        Date expiration = jwtService.extractClaim(
                token,
                Claims::getExpiration
        );

        assertNotNull(expiration);
        assertTrue(expiration.after(new Date()));
    }

    @Test
    void isTokenValidShouldReturnTrueForValidTokenAndMatchingUser() {
        String token = jwtService.generateToken(userDetails);

        boolean result =
                jwtService.isTokenValid(
                        token,
                        userDetails
                );

        assertTrue(result);
    }

    @Test
    void isTokenValidShouldReturnFalseForDifferentUser() {
        String token = jwtService.generateToken(userDetails);

        UserDetails differentUser =
                User.withUsername("differentuser")
                        .password("password")
                        .roles("EMPLOYEE")
                        .build();

        boolean result =
                jwtService.isTokenValid(
                        token,
                        differentUser
                );

        assertFalse(result);
    }

    @Test
    void isTokenValidShouldReturnFalseForExpiredToken() {
        ReflectionTestUtils.setField(
                jwtService,
                "jwtExpiration",
                -1000L
        );

        String token = jwtService.generateToken(userDetails);

        assertThrows(
                Exception.class,
                () -> jwtService.isTokenValid(
                        token,
                        userDetails
                )
        );
    }

    @Test
    void extractUsernameShouldRejectTamperedToken() {
        String token = jwtService.generateToken(userDetails);

        String tamperedToken =
                token.substring(0, token.length() - 2) + "xx";

        assertThrows(
                Exception.class,
                () -> jwtService.extractUsername(tamperedToken)
        );
    }

    @Test
    void extractUsernameShouldRejectMalformedToken() {
        assertThrows(
                Exception.class,
                () -> jwtService.extractUsername("invalid-token")
        );
    }
}
