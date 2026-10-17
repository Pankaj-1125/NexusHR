package com.nexushr.controller;

import com.nexushr.entity.User;
import com.nexushr.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Set<String> ALLOWED_ROLES =
            Set.of("ADMIN", "HR", "MANAGER", "EMPLOYEE");

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateRole(
            @PathVariable Long id,
            @RequestBody RoleRequest request
    ) {

        if (request == null ||
                request.role() == null ||
                request.role().isBlank()) {

            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Role is required"));
        }

        String role = request.role().trim().toUpperCase();

        if (!ALLOWED_ROLES.contains(role)) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "Invalid role. Allowed roles: ADMIN, HR, MANAGER, EMPLOYEE"
                    ));
        }

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("User not found with id: " + id));

        user.setRole(role);

        User savedUser = userRepository.save(user);

        return ResponseEntity.ok(
                new RoleResponse(
                        savedUser.getId(),
                        savedUser.getUsername(),
                        savedUser.getRole(),
                        savedUser.isEnabled()
                )
        );
    }

    public record RoleRequest(String role) {
    }

    public record RoleResponse(
            Long id,
            String username,
            String role,
            boolean enabled
    ) {
    }
}