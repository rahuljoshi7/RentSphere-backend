package com.rentsphere.dto.response;

import com.rentsphere.entity.Facility.FacilityStatus;
import com.rentsphere.entity.Facility.FacilityType;
import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data @Builder
public class FacilityResponse {
    private Long id;
    private Long propertyId;
    private FacilityType facilityType;
    private FacilityStatus status;
    private String description;
    private Instant createdAt;
}
