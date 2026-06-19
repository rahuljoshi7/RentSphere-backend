package com.rentsphere.dto.response;

import com.rentsphere.entity.Report;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ReportResponse {
    private Long               id;
    private String             generatedByName;
    private Report.ReportType  reportType;
    private Report.ReportFormat format;
    private Instant            generatedAt;

    public static ReportResponse from(Report r) {
        return ReportResponse.builder()
            .id(r.getId())
            .generatedByName(r.getGeneratedBy().getFullName())
            .reportType(r.getReportType())
            .format(r.getFormat())
            .generatedAt(r.getGeneratedAt())
            .build();
    }
}
