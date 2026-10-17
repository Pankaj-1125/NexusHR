package com.nexushr.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter
        extends OncePerRequestFilter {

    private static final Logger logger =
            LoggerFactory.getLogger(
                    JwtAuthenticationFilter.class
            );

    private final JwtService jwtService;

    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserDetailsService userDetailsService
    ) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        String jwt = null;
        String tokenSource = null;

        if (authHeader != null && !authHeader.isBlank()) {
            if (authHeader.startsWith("Bearer ")) {
                jwt = authHeader.substring(7).trim();
                tokenSource = "bearer";
            } else {
                logger.debug("Authorization header is not a Bearer token");
            }
        }

        // Same-origin browser fallback. The login endpoint also issues an
        // HttpOnly cookie so protected APIs still authenticate if a browser
        // loses localStorage or an older frontend build does not attach the
        // Authorization header correctly.
        if (jwt == null || jwt.isBlank()) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("nexushr_access_token".equals(cookie.getName())) {
                        jwt = cookie.getValue();
                        tokenSource = "cookie";
                        break;
                    }
                }
            }
        }

        if (jwt == null || jwt.isBlank()) {
            logger.debug(
                    "No JWT found for {} {}",
                    request.getMethod(),
                    request.getRequestURI()
            );
            filterChain.doFilter(request, response);
            return;
        }

        final String resolvedJwt = jwt;

        if (jwt.isBlank()) {

            logger.debug(
                    "Bearer token is empty"
            );

            filterChain.doFilter(request, response);
            return;
        }

        try {

            final String username =
                    jwtService.extractUsername(resolvedJwt);

            if (username == null ||
                    username.isBlank()) {

                logger.warn(
                        "JWT did not contain a username"
                );

                filterChain.doFilter(request, response);
                return;
            }

            if (SecurityContextHolder
                    .getContext()
                    .getAuthentication() == null) {

                UserDetails userDetails =
                        userDetailsService
                                .loadUserByUsername(username);

                if (!userDetails.isEnabled()) {

                    logger.warn(
                            "JWT rejected because user is disabled: {}",
                            username
                    );

                    filterChain.doFilter(request, response);
                    return;
                }

                if (jwtService.isTokenValid(
                        resolvedJwt,
                        userDetails
                )) {

                    UsernamePasswordAuthenticationToken
                            authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    authentication.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(request)
                    );

                    SecurityContextHolder
                            .getContext()
                            .setAuthentication(authentication);

                    logger.debug(
                            "JWT authentication successful for user: {} via {}",
                            username,
                            tokenSource
                    );

                } else {

                    logger.warn(
                            "JWT validation failed for user: {}",
                            username
                    );
                }

            } else {

                logger.debug(
                        "SecurityContext already contains authentication"
                );
            }

        } catch (Exception exception) {

            logger.warn(
                    "JWT authentication failed: {}",
                    exception.getMessage()
            );
        }

        filterChain.doFilter(request, response);
    }
}