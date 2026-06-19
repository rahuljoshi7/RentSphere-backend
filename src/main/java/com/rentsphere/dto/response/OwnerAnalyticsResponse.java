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
public class OwnerAnalyticsResponse {
    private List<RevenueTrendPoint> revenueTrend;
    private PaymentStatusBreakdown paymentStatusBreakdown;
    private List<TopPropertyResponse> topProperties;
    private long agreementsExpiringSoon;
}
