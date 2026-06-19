package com.rentsphere.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data @Builder
public class AdminDashboardResponse {
    private long totalProperties;
    private long occupiedProperties;
    private long vacantProperties;
    private long totalTenants;
    private BigDecimal monthlyRevenue;
    private long pendingPayments;
    private long openMaintenanceRequests;
    private long activeAgreements;
}
