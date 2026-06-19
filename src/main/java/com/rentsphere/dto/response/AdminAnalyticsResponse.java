package com.rentsphere.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAnalyticsResponse {
    private List<RevenueTrendPoint> revenueTrend;
    private PaymentStatusBreakdown paymentStatusBreakdown;
    private MaintenancePriorityBreakdown maintenancePriorityBreakdown;
    private List<TopPropertyResponse> topProperties;
    private long agreementsExpiringSoon;
}
