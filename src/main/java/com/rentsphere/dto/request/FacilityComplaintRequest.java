package com.rentsphere.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FacilityComplaintRequest {
    @NotNull  private Long facilityId;
    @NotBlank private String complaintText;
}
