package com.rentsphere.dto.response;

import com.rentsphere.entity.MaintenanceRequest.MaintenanceStatus;
import com.rentsphere.entity.MaintenanceRequest.Priority;
import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.List;

@Data @Builder
public class MaintenanceResponse {
    private Long id;
    private Long propertyId;
    private String propertyName;
    private Long tenantId;
    private String tenantName;
    private String title;
    private String description;
    private MaintenanceStatus status;
    private Priority priority;
    private String imageUrl;
    private List<AssignmentInfo> assignments;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant closedAt;

    @Data @Builder
    public static class AssignmentInfo {
        private Long staffId;
        private String staffName;
        private String notes;
        private Instant assignedAt;
        private Instant completedAt;
    }
}
