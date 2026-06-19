package com.rentsphere.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopPropertyResponse {
    private Long propertyId;
    private String propertyName;
    private String city;
    private BigDecimal totalRevenue;
}
