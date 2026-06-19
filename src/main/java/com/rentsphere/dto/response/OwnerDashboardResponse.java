package com.rentsphere.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data @Builder
public class OwnerDashboardResponse {
    private long totalProperties;
    private long occupiedProperties;
    private long vacantProperties;
    private long totalTenants;
    private BigDecimal monthlyRevenue;
    private BigDecimal yearlyRevenue;
    private double occupancyRate;
}
