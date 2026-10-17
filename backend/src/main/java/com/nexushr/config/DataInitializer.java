package com.nexushr.config;

import com.nexushr.entity.User;
import com.nexushr.repository.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Value("${nexushr.seed-default-users:false}")
    private boolean seedDefaultUsers;

    @Value("${nexushr.seed.admin.username:admin}")
    private String adminUsername;

    @Value("${nexushr.seed.admin.password:}")
    private String adminPassword;

    @Value("${nexushr.seed.manager.username:manager}")
    private String managerUsername;

    @Value("${nexushr.seed.manager.password:}")
    private String managerPassword;

    @Value("${nexushr.seed.employee.username:employee}")
    private String employeeUsername;

    @Value("${nexushr.seed.employee.password:}")
    private String employeePassword;

    @Bean
    CommandLineRunner createDefaultUsers(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {

            if (!seedDefaultUsers) {
                return;
            }

            createUserIfMissing(
                    userRepository,
                    passwordEncoder,
                    adminUsername,
                    adminPassword,
                    "ADMIN"
            );

            createUserIfMissing(
                    userRepository,
                    passwordEncoder,
                    managerUsername,
                    managerPassword,
                    "MANAGER"
            );

            createUserIfMissing(
                    userRepository,
                    passwordEncoder,
                    employeeUsername,
                    employeePassword,
                    "EMPLOYEE"
            );
        };
    }

    private void createUserIfMissing(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            String username,
            String rawPassword,
            String role
    ) {

        if (username == null
                || username.isBlank()
                || rawPassword == null
                || rawPassword.isBlank()) {

            return;
        }

        String cleanUsername =
                username.trim();

        if (userRepository
                .findByUsername(cleanUsername)
                .isPresent()) {

            return;
        }

        User user = new User();

        user.setUsername(
                cleanUsername
        );

        user.setPassword(
                passwordEncoder.encode(
                        rawPassword
                )
        );

        user.setRole(
                role
        );

        user.setEnabled(
                true
        );

        userRepository.save(
                user
        );
    }
}