package com.rentsphere.service;

import com.rentsphere.dto.response.PagedResponse;
import com.rentsphere.dto.response.UserResponse;
import com.rentsphere.entity.Role;
import com.rentsphere.entity.User;
import com.rentsphere.exception.ResourceNotFoundException;
import com.rentsphere.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final UserRepository userRepository;

    // ── Queries ───────────────────────────────────────────────────────────────

    public PagedResponse<UserResponse> listUsers(int page, int size) {
        var result = userRepository.findAll(
            PageRequest.of(page, size, Sort.by("createdAt").descending())
        );
        return PagedResponse.of(result.map(UserResponse::from));
    }

    public PagedResponse<UserResponse> searchUsers(String q, int page, int size) {
        var result = userRepository.search(q, PageRequest.of(page, size));
        return PagedResponse.of(result.map(UserResponse::from));
    }

    public PagedResponse<UserResponse> listByRole(Role.RoleName role, int page, int size) {
        var result = userRepository.findByRole(role, PageRequest.of(page, size));
        return PagedResponse.of(result.map(UserResponse::from));
    }

    public UserResponse getUserById(Long id) {
        User user = findOrThrow(id);
        return UserResponse.from(user);
    }

    // ── Commands ──────────────────────────────────────────────────────────────

    /**
     * Activate a user account.
     * Place any cascade side-effects here (e.g. re-enable their agreements,
     * send a welcome-back notification) as the product grows.
     */
    @Transactional
    public void activateUser(Long id) {
        User user = findOrThrow(id);
        if (Boolean.TRUE.equals(user.getIsActive())) {
            log.debug("User {} is already active — no-op", id);
            return;
        }
        user.setIsActive(true);
        userRepository.save(user);
        log.info("Admin activated user {}", id);
    }

    /**
     * Deactivate a user account.
     * Place cascade logic here (e.g. expire active sessions, suspend agreements)
     * when that behaviour is required.
     */
    @Transactional
    public void deactivateUser(Long id) {
        User user = findOrThrow(id);
        if (Boolean.FALSE.equals(user.getIsActive())) {
            log.debug("User {} is already inactive — no-op", id);
            return;
        }
        user.setIsActive(false);
        userRepository.save(user);
        log.info("Admin deactivated user {}", id);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private User findOrThrow(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
}
