package com.rentsphere.config;

import com.rentsphere.entity.Role;
import com.rentsphere.entity.User;
import com.rentsphere.repository.RoleRepository;
import com.rentsphere.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.Set;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final RoleRepository  roleRepository;
    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner seedData() {
        return args -> {
            seedRoles();
            seedAdminUser();
        };
    }

    // ── Roles ─────────────────────────────────────────────────────────────────

    private void seedRoles() {
        Arrays.stream(Role.RoleName.values()).forEach(roleName -> {
            if (roleRepository.findByName(roleName).isEmpty()) {
                roleRepository.save(Role.builder()
                    .name(roleName)
                    .description(descriptionFor(roleName))
                    .build());
                log.info("Seeded role: {}", roleName);
            }
        });
    }

    // ── Default Admin ─────────────────────────────────────────────────────────

    private void seedAdminUser() {
        String adminEmail = "admin@rentsphere.com";
        if (!userRepository.existsByEmail(adminEmail)) {
            Role adminRole = roleRepository.findByName(Role.RoleName.ADMIN)
                .orElseThrow(() -> new IllegalStateException("ADMIN role not found after seeding"));

            User admin = User.builder()
                .firstName("System")
                .lastName("Admin")
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode("Admin@1234"))
                .phone("+91-0000000000")
                .isActive(true)
                .roles(Set.of(adminRole))
                .build();

            userRepository.save(admin);
            log.info("Default admin user created: {} (change password immediately!)", adminEmail);
        }
    }

    private String descriptionFor(Role.RoleName name) {
        return switch (name) {
            case ADMIN             -> "System administrator with full access";
            case PROPERTY_OWNER    -> "Owner of one or more properties";
            case PROPERTY_MANAGER  -> "Manages properties on behalf of owners";
            case TENANT            -> "Rents a property";
            case MAINTENANCE_STAFF -> "Handles maintenance requests";
        };
    }
}
