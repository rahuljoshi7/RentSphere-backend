package com.rentsphere.dto.request;

import com.rentsphere.entity.Facility.FacilityStatus;
import com.rentsphere.entity.Facility.FacilityType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FacilityRequest {
    @NotNull private FacilityType facilityType;
    private FacilityStatus status = FacilityStatus.ACTIVE;
    private String description;
}
