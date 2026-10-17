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

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value(
            "${nexushr.cors.allowed-origins:http://localhost:5173,http://localhost}"
    )
    private String allowedOrigins;


    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }


    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }


    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {

        return configuration.getAuthenticationManager();
    }


    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http

                // =====================================================
                // CSRF
                // =====================================================

                .csrf(
                        csrf -> csrf.disable()
                )


                // =====================================================
                // CORS
                // =====================================================

                .cors(
                        cors ->
                                cors.configurationSource(
                                        corsConfigurationSource()
                                )
                )


                // =====================================================
                // SESSION
                // JWT based stateless security
                // =====================================================

                .sessionManagement(
                        session ->
                                session.sessionCreationPolicy(
                                        SessionCreationPolicy.STATELESS
                                )
                )


                // =====================================================
                // AUTHENTICATION / AUTHORIZATION ERRORS
                // =====================================================

                .exceptionHandling(
                        exceptions -> exceptions

                                .authenticationEntryPoint(
                                        (
                                                request,
                                                response,
                                                authException
                                        ) -> {

                                            response.setStatus(
                                                    HttpServletResponse.SC_UNAUTHORIZED
                                            );

                                            response.setContentType(
                                                    "application/json"
                                            );

                                            response.getWriter().write(
                                                    "{\"message\":\"Authentication required or token is invalid.\"}"
                                            );
                                        }
                                )

                                .accessDeniedHandler(
                                        (
                                                request,
                                                response,
                                                accessDeniedException
                                        ) -> {

                                            response.setStatus(
                                                    HttpServletResponse.SC_FORBIDDEN
                                            );

                                            response.setContentType(
                                                    "application/json"
                                            );

                                            response.getWriter().write(
                                                    "{\"message\":\"Access denied for the current role.\"}"
                                            );
                                        }
                                )
                )


                // =====================================================
                // AUTHORIZATION RULES
                // =====================================================

                .authorizeHttpRequests(
                        auth -> auth


                                // =================================================
                                // PUBLIC ENDPOINTS
                                // =================================================

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


                                // =================================================
                                // CURRENT LOGGED-IN USER
                                // =================================================

                                .requestMatchers(
                                        "/api/auth/me"
                                )
                                .authenticated()


                                // =================================================
                                // SELF SERVICE
                                // =================================================

                                .requestMatchers(
                                        "/api/me/**"
                                )
                                .authenticated()


                                // =================================================
                                // USER MANAGEMENT
                                // ADMIN ONLY
                                // =================================================

                                .requestMatchers(
                                        "/api/users/**"
                                )
                                .hasRole(
                                        "ADMIN"
                                )


                                // =================================================
                                // AUDIT LOGS
                                // ADMIN / HR
                                // =================================================

                                .requestMatchers(
                                        "/api/audit-logs/**"
                                )
                                .hasAnyRole(
                                        "ADMIN",
                                        "HR"
                                )


                                // =================================================
                                // EMPLOYEE DOCUMENTS
                                // ADMIN / HR / MANAGER
                                // =================================================

                                .requestMatchers(
                                        "/api/employees/*/documents/**"
                                )
                                .hasAnyRole(
                                        "ADMIN",
                                        "HR",
                                        "MANAGER"
                                )


                                // =================================================
                                // EMPLOYEE READ
                                // ADMIN / HR / MANAGER
                                // =================================================

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/employees/**"
                                )
                                .hasAnyRole(
                                        "ADMIN",
                                        "HR",
                                        "MANAGER"
                                )


                                // =================================================
                                // EMPLOYEE CREATE
                                // ADMIN / HR ONLY
                                //
                                // MANAGER CANNOT CREATE EMPLOYEES
                                // =================================================

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/employees/**"
                                )
                                .hasAnyRole(
                                        "ADMIN",
                                        "HR"
                                )


                                // =================================================
                                // EMPLOYEE UPDATE
                                //
                                // ADMIN / HR / MANAGER
                                //
                                // Manager is allowed to edit employee details
                                // =================================================

                                .requestMatchers(
                                        HttpMethod.PUT,
                                        "/api/employees/**"
                                )
                                .hasAnyRole(
                                        "ADMIN",
                                        "HR",
                                        "MANAGER"
                                )


                                // PATCH support
                                // Useful if frontend uses PATCH later
                                .requestMatchers(
                                        HttpMethod.PATCH,
                                        "/api/employees/**"
                                )
                                .hasAnyRole(
                                        "ADMIN",
                                        "HR",
                                        "MANAGER"
                                )


                                // =================================================
                                // EMPLOYEE DELETE
                                //
                                // ADMIN ONLY
                                //
                                // Manager MUST NOT delete employees
                                // =================================================

                                .requestMatchers(
                                        HttpMethod.DELETE,
                                        "/api/employees/**"
                                )
                                .hasRole(
                                        "ADMIN"
                                )


                                // =================================================
                                // ATTENDANCE SELF SERVICE
                                // =================================================

                                .requestMatchers(
                                        HttpMethod.POST,

                                        "/api/attendance/check-in/**",

                                        "/api/attendance/check-out/**"
                                )
                                .authenticated()


                                // =================================================
                                // EMPLOYEE ATTENDANCE HISTORY
                                // =================================================

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/attendance/employee/**"
                                )
                                .authenticated()


                                // =================================================
                                // ATTENDANCE MANAGEMENT - READ
                                //
                                // ADMIN / HR / MANAGER
                                // =================================================

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/attendance/**"
                                )
                                .hasAnyRole(
                                        "ADMIN",
                                        "HR",
                                        "MANAGER"
                                )


                                // =================================================
                                // ATTENDANCE MANAGEMENT - CREATE
                                //
                                // ADMIN / HR / MANAGER
                                // =================================================

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/attendance/**"
                                )
                                .hasAnyRole(
                                        "ADMIN",
                                        "HR",
                                        "MANAGER"
                                )


                                // =================================================
                                // ATTENDANCE UPDATE
                                //
                                // ADMIN / HR
                                // =================================================

                                .requestMatchers(
                                        HttpMethod.PUT,
                                        "/api/attendance/**"
                                )
                                .hasAnyRole(
                                        "ADMIN",
                                        "HR"
                                )


                                // =================================================
                                // ATTENDANCE DELETE
                                //
                                // ADMIN / HR
                                // =================================================

                                .requestMatchers(
                                        HttpMethod.DELETE,
                                        "/api/attendance/**"
                                )
                                .hasAnyRole(
                                        "ADMIN",
                                        "HR"
                                )


                                // =================================================
                                // ALL OTHER API ENDPOINTS
                                // =================================================

                                .requestMatchers(
                                        "/api/**"
                                )
                                .authenticated()


                                // =================================================
                                // EVERYTHING ELSE
                                // =================================================

                                .anyRequest()
                                .authenticated()
                )


                // =====================================================
                // JWT FILTER
                // =====================================================

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );


        return http.build();
    }


    // =============================================================
    // CORS CONFIGURATION
    // =============================================================

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();


        // ---------------------------------------------------------
        // Read comma-separated allowed origins from environment
        //
        // Local:
        // http://localhost:5173,http://localhost
        //
        // Render:
        // https://your-frontend.onrender.com
        // ---------------------------------------------------------

        List<String> origins =
                Arrays.stream(
                                allowedOrigins.split(",")
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


        // ---------------------------------------------------------
        // HTTP METHODS
        // ---------------------------------------------------------

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


        // ---------------------------------------------------------
        // REQUEST HEADERS
        // ---------------------------------------------------------

        configuration.setAllowedHeaders(
                List.of(
                        "*"
                )
        );


        // ---------------------------------------------------------
        // AUTHORIZATION / CREDENTIAL SUPPORT
        // ---------------------------------------------------------

        configuration.setAllowCredentials(
                true
        );


        // ---------------------------------------------------------
        // REGISTER CORS FOR ALL ENDPOINTS
        // ---------------------------------------------------------

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();


        source.registerCorsConfiguration(
                "/**",
                configuration
        );


        return source;
    }
}