package com.rentsphere.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TenantProfileRequest {
    @Size(max = 100) private String aadhaarRef;
    @Size(max = 50)  private String panRef;
    private String currentAddress;
    @Size(max = 150) private String emergencyContactName;
    @Size(max = 20)  private String emergencyContactPhone;
}
