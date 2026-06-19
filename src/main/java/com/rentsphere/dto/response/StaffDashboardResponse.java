package com.rentsphere.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffDashboardResponse {
    private long totalAssigned;
    private long openCount;
    private long inProgressCount;
    private long completedCount;
    private MaintenancePriorityBreakdown priorityBreakdown;
}
