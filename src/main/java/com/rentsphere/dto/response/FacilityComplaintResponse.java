package com.rentsphere.dto.response;

import com.rentsphere.entity.FacilityComplaint.ComplaintStatus;
import com.rentsphere.entity.Facility.FacilityType;
import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data @Builder
public class FacilityComplaintResponse {
    private Long id;
    private Long facilityId;
    private FacilityType facilityType;
    private Long tenantId;
    private String tenantName;
    private String complaintText;
    private ComplaintStatus status;
    private Instant createdAt;
    private Instant resolvedAt;
}
