package com.rentsphere.service.impl;

import com.rentsphere.dto.request.LoginRequest;
import com.rentsphere.dto.request.RegisterRequest;
import com.rentsphere.dto.response.AuthResponse;
import com.rentsphere.entity.Role;
import com.rentsphere.entity.Tenant;
import com.rentsphere.entity.User;
import com.rentsphere.exception.BusinessException;
import com.rentsphere.exception.DuplicateResourceException;
import com.rentsphere.exception.ResourceNotFoundException;
import com.rentsphere.repository.RoleRepository;
import com.rentsphere.repository.TenantRepository;
import com.rentsphere.repository.UserRepository;
import com.rentsphere.security.JwtService;
import com.rentsphere.service.AuthService;
import com.rentsphere.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository         userRepository;
    private final RoleRepository         roleRepository;
    private final TenantRepository       tenantRepository;
    private final PasswordEncoder        passwordEncoder;
    private final JwtService             jwtService;
    private final AuthenticationManager  authenticationManager;
    private final NotificationService    notificationService;

    // ── Register ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already in use: " + request.getEmail());
        }

        Role role = roleRepository.findByName(request.getRole())
            .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + request.getRole()));

        User user = User.builder()
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .phone(request.getPhone())
            .isActive(true)
            .roles(Set.of(role))
            .build();

        user = userRepository.save(user);

        // Auto-create Tenant profile when the registering role is TENANT
        if (request.getRole() == Role.RoleName.TENANT) {
            Tenant tenant = Tenant.builder().user(user).build();
            tenantRepository.save(tenant);
            try {
                notificationService.sendTenantRegistrationNotification(user);
            } catch (Exception e) {
                log.error("Email notification failed", e);
            }
        }

        log.info("New user registered: {} [{}]", user.getEmail(), role.getName());

        String token = jwtService.generateToken(user);
        return buildAuthResponse(user, token);
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Override
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new ResourceNotFoundException("User", null));

        if (!user.isEnabled()) {
            throw new BusinessException("Account is deactivated. Please contact support.");
        }

        String token = jwtService.generateToken(user);
        log.info("User logged in: {}", user.getEmail());
        return buildAuthResponse(user, token);
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private AuthResponse buildAuthResponse(User user, String token) {
        Set<Role.RoleName> roles = user.getRoles().stream()
            .map(Role::getName)
            .collect(Collectors.toSet());

        return AuthResponse.builder()
            .accessToken(token)
            .tokenType("Bearer")
            .userId(user.getId())
            .email(user.getEmail())
            .fullName(user.getFullName())
            .roles(roles)
            .build();
    }
}
