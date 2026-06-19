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
public class RevenueTrendPoint {
    private int month;
    private int year;
    private String label;        // e.g. "Jan 2026"
    private BigDecimal revenue;
}
