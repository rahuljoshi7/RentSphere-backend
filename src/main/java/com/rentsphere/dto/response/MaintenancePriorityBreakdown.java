package com.rentsphere.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenancePriorityBreakdown {
    private long low;
    private long medium;
    private long high;
    private long urgent;
}
