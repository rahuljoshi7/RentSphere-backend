package com.rentsphere.dto.response;

import com.rentsphere.entity.Role;
import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class AuthResponse {
    private String accessToken;
    private String tokenType;
    private Long   userId;
    private String email;
    private String fullName;
    private Set<Role.RoleName> roles;
}
