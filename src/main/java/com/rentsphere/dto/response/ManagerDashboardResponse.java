package com.rentsphere.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagerDashboardResponse {
    private long managedProperties;
    private long occupiedProperties;
    private long vacantProperties;
    private long activeTenants;
    private long activeAgreements;
    private long openMaintenanceRequests;
    private long pendingPayments;
    private double occupancyRate;
}
