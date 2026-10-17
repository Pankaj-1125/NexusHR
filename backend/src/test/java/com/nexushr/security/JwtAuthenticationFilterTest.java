package com.nexushr.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private FilterChain filterChain;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private JwtAuthenticationFilter filter;

    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService);

        userDetails = User.withUsername("testuser")
                .password("password")
                .roles("EMPLOYEE")
                .build();

        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldContinueFilterChainWhenAuthorizationHeaderIsMissing()
            throws ServletException, IOException {

        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());

        verify(request).getHeader("Authorization");
        verify(filterChain).doFilter(request, response);

        verifyNoInteractions(jwtService);
        verifyNoInteractions(userDetailsService);
    }

    @Test
    void shouldContinueFilterChainWhenAuthorizationHeaderIsNotBearer()
            throws ServletException, IOException {

        when(request.getHeader("Authorization"))
                .thenReturn("Basic some-token");

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());

        verify(request).getHeader("Authorization");
        verify(filterChain).doFilter(request, response);

        verifyNoInteractions(jwtService);
        verifyNoInteractions(userDetailsService);
    }

    @Test
    void shouldAuthenticateUserWhenTokenIsValid()
            throws ServletException, IOException {

        String token = "valid-jwt-token";

        when(request.getHeader("Authorization"))
                .thenReturn("Bearer " + token);

        when(jwtService.extractUsername(token))
                .thenReturn("testuser");

        when(userDetailsService.loadUserByUsername("testuser"))
                .thenReturn(userDetails);

        when(jwtService.isTokenValid(token, userDetails))
                .thenReturn(true);

        filter.doFilter(request, response, filterChain);

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        assertNotNull(authentication);
        assertTrue(authentication instanceof UsernamePasswordAuthenticationToken);
        assertEquals("testuser", authentication.getName());
        assertTrue(authentication.isAuthenticated());

        assertEquals(
                List.of("ROLE_EMPLOYEE"),
                authentication.getAuthorities()
                        .stream()
                        .map(authority -> authority.getAuthority())
                        .toList()
        );

        verify(jwtService).extractUsername(token);
        verify(userDetailsService).loadUserByUsername("testuser");
        verify(jwtService).isTokenValid(token, userDetails);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotAuthenticateWhenTokenIsInvalid()
            throws ServletException, IOException {

        String token = "invalid-jwt-token";

        when(request.getHeader("Authorization"))
                .thenReturn("Bearer " + token);

        when(jwtService.extractUsername(token))
                .thenReturn("testuser");

        when(userDetailsService.loadUserByUsername("testuser"))
                .thenReturn(userDetails);

        when(jwtService.isTokenValid(token, userDetails))
                .thenReturn(false);

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());

        verify(jwtService).extractUsername(token);
        verify(userDetailsService).loadUserByUsername("testuser");
        verify(jwtService).isTokenValid(token, userDetails);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldContinueFilterChainWhenUsernameCannotBeExtracted()
            throws ServletException, IOException {

        String token = "token-without-username";

        when(request.getHeader("Authorization"))
                .thenReturn("Bearer " + token);

        when(jwtService.extractUsername(token))
                .thenReturn(null);

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());

        verify(jwtService).extractUsername(token);
        verifyNoInteractions(userDetailsService);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldContinueFilterChainWhenJwtExtractionThrowsException()
            throws ServletException, IOException {

        String token = "malformed-token";

        when(request.getHeader("Authorization"))
                .thenReturn("Bearer " + token);

        when(jwtService.extractUsername(token))
                .thenThrow(new RuntimeException("Invalid JWT"));

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());

        verify(jwtService).extractUsername(token);
        verifyNoInteractions(userDetailsService);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldContinueFilterChainWhenUserLookupFails()
            throws ServletException, IOException {

        String token = "valid-token";

        when(request.getHeader("Authorization"))
                .thenReturn("Bearer " + token);

        when(jwtService.extractUsername(token))
                .thenReturn("testuser");

        when(userDetailsService.loadUserByUsername("testuser"))
                .thenThrow(new RuntimeException("User not found"));

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());

        verify(jwtService).extractUsername(token);
        verify(userDetailsService)
                .loadUserByUsername("testuser");

        verify(jwtService, never())
                .isTokenValid(anyString(), any(UserDetails.class));

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotReplaceExistingAuthentication()
            throws ServletException, IOException {

        Authentication existingAuthentication =
                new UsernamePasswordAuthenticationToken(
                        "existing-user",
                        null,
                        List.of()
                );

        SecurityContextHolder.getContext()
                .setAuthentication(existingAuthentication);

        String token = "valid-token";

        when(request.getHeader("Authorization"))
                .thenReturn("Bearer " + token);

        when(jwtService.extractUsername(token))
                .thenReturn("testuser");

        filter.doFilter(request, response, filterChain);

        assertSame(
                existingAuthentication,
                SecurityContextHolder.getContext().getAuthentication()
        );

        verify(jwtService).extractUsername(token);
        verifyNoInteractions(userDetailsService);

        verify(jwtService, never())
                .isTokenValid(anyString(), any(UserDetails.class));

        verify(filterChain).doFilter(request, response);
    }
}
