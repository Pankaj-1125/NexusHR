package com.nexushr.config;

import com.nexushr.security.JwtAuthenticationFilter;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpMethod;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.AuthenticationManager;

import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;

import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.cors.CorsConfiguration;

import org.springframework.web.cors.CorsConfigurationSource;

import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@EnableMethodSecurity
@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter
            jwtAuthenticationFilter;

    @Value(
            "${nexushr.cors.allowed-origins:http://localhost:5173,http://localhost}"
    )
    private String allowedOrigins;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter
    ) {

        this.jwtAuthenticationFilter =
                jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {

        return configuration
                .getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http

                .csrf(
                        csrf ->
                                csrf.disable()
                )

                .cors(
                        cors ->
                                cors.configurationSource(
                                        corsConfigurationSource()
                                )
                )

                .sessionManagement(
                        session ->
                                session.sessionCreationPolicy(
                                        SessionCreationPolicy.STATELESS
                                )
                )

                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write(
                                    "{\"message\":\"Authentication required or token is invalid.\"}"
                            );
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.getWriter().write(
                                    "{\"message\":\"Access denied for the current role.\"}"
                            );
                        })
                )

                .authorizeHttpRequests(
                        auth -> auth

                                // ==============================
                                // PUBLIC
                                // ==============================

                                .requestMatchers(
                                        "/api/health",
                                        "/api/auth/login",
                                        "/api/auth/refresh",
                                        "/api/auth/logout",
                                        "/api/auth/forgot-password",
                                        "/api/auth/reset-password",
                                        "/api/public/**",
                                        "/actuator/health",
                                        "/error"
                                )
                                .permitAll()

                                // ==============================
                                // CURRENT USER
                                // ==============================

                                .requestMatchers(
                                        "/api/auth/me"
                                )
                                .authenticated()

                                // ==============================
                                // AUTHENTICATED SELF SERVICE
                                // ==============================

                                .requestMatchers(
                                        "/api/me/**"
                                )
                                .authenticated()

                                // ==============================
                                // USER MANAGEMENT
                                // ==============================

                                .requestMatchers(
                                        "/api/users/**"
                                )
                                .hasRole(
                                        "ADMIN"
                                )

                                // ==============================
                                // AUDIT LOGS
                                // ==============================

                                .requestMatchers(
                                        "/api/audit-logs/**"
                                )
                                .hasAnyRole(
                                        "ADMIN",
                                        "HR"
                                )

                                // ==============================
                                // EMPLOYEE DOCUMENTS
                                // ==============================

                                .requestMatchers(
                                        "/api/employees/*/documents/**"
                                )
                                .hasAnyRole(
                                        "ADMIN",
                                        "HR",
                                        "MANAGER"
                                )

                                // ==============================
                                // EMPLOYEE READ
                                // ==============================

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/employees/**"
                                )
                                .hasAnyRole(
                                        "ADMIN",
                                        "HR",
                                        "MANAGER"
                                )

                                // ==============================
                                // EMPLOYEE DELETE
                                // ADMIN + MANAGER
                                // ==============================

                                .requestMatchers(
                                        HttpMethod.DELETE,
                                        "/api/employees/**"
                                )
                                .hasAnyRole(
                                        "ADMIN",
                                        "MANAGER"
                                )

                                // ==============================
                                // EMPLOYEE CREATE
                                // ==============================

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/employees/**"
                                )
                                .hasAnyRole(
                                        "ADMIN",
                                        "HR"
                                )

                                // ==============================
                                // EMPLOYEE UPDATE
                                // ==============================

                                .requestMatchers(
                                        HttpMethod.PUT,
                                        "/api/employees/**"
                                )
                                .hasAnyRole(
                                        "ADMIN",
                                        "HR"
                                )

                                // ==============================
                                // ATTENDANCE SELF SERVICE
                                // ==============================

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/attendance/check-in/**",
                                        "/api/attendance/check-out/**"
                                )
                                .authenticated()

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/attendance/employee/**"
                                )
                                .authenticated()

                                // ==============================
                                // ATTENDANCE MANAGEMENT
                                // ==============================

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/attendance/**"
                                )
                                .hasAnyRole(
                                        "ADMIN",
                                        "HR",
                                        "MANAGER"
                                )

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/attendance/**"
                                )
                                .hasAnyRole(
                                        "ADMIN",
                                        "HR",
                                        "MANAGER"
                                )

                                .requestMatchers(
                                        HttpMethod.PUT,
                                        "/api/attendance/**"
                                )
                                .hasAnyRole(
                                        "ADMIN",
                                        "HR"
                                )

                                .requestMatchers(
                                        HttpMethod.DELETE,
                                        "/api/attendance/**"
                                )
                                .hasAnyRole(
                                        "ADMIN",
                                        "HR"
                                )

                                // ==============================
                                // EVERYTHING ELSE
                                // ==============================

                                .requestMatchers(
                                        "/api/**"
                                )
                                .authenticated()

                                .anyRequest()
                                .authenticated()
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    // =========================================================
    // CORS
    // =========================================================

    @Bean
    public CorsConfigurationSource
    corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();

        List<String> origins =
                Arrays.stream(
                                allowedOrigins.split(
                                        ","
                                )
                        )
                        .map(
                                String::trim
                        )
                        .filter(
                                origin ->
                                        !origin.isBlank()
                        )
                        .toList();

        configuration.setAllowedOrigins(
                origins
        );

        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );

        configuration.setAllowedHeaders(
                List.of(
                        "*"
                )
        );

        configuration.setAllowCredentials(
                true
        );

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }
}