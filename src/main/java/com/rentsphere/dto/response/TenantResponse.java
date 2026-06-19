package com.rentsphere.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data @Builder
public class TenantResponse {
    private Long id;
    private Long userId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String aadhaarRef;
    private String panRef;
    private String currentAddress;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private Instant createdAt;
}
