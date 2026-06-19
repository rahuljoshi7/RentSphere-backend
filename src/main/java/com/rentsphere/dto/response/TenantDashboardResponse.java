package com.rentsphere.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantDashboardResponse {
    private String activePropertyName;
    private LocalDate agreementEndDate;
    private BigDecimal monthlyRent;
    private long pendingPayments;
    private BigDecimal totalDue;
    private long openMaintenanceRequests;
    private long totalMaintenanceRequests;
    private boolean agreementExpiringSoon;
}
