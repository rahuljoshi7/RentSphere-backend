package com.rentsphere.dto.response;

import com.rentsphere.entity.Role;
import com.rentsphere.entity.User;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Builder
public class UserResponse {
    private Long              id;
    private String            email;
    private String            firstName;
    private String            lastName;
    private String            phone;
    private boolean           isActive;
    private Set<Role.RoleName> roles;
    private Instant           createdAt;

    public static UserResponse from(User u) {
        return UserResponse.builder()
            .id(u.getId())
            .email(u.getEmail())
            .firstName(u.getFirstName())
            .lastName(u.getLastName())
            .phone(u.getPhone())
            .isActive(Boolean.TRUE.equals(u.getIsActive()))
            .roles(u.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
            .createdAt(u.getCreatedAt())
            .build();
    }
}
