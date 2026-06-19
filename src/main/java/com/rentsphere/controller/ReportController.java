package com.rentsphere.controller;

import com.rentsphere.dto.response.PagedResponse;
import com.rentsphere.dto.response.ReportResponse;
import com.rentsphere.entity.Report;
import com.rentsphere.service.ReportService;
import com.rentsphere.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Generate and download business reports")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private final ReportService reportService;

    @PostMapping("/revenue")
    @Operation(summary = "Generate revenue report")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER')")
    public ResponseEntity<byte[]> revenueReport(
        @RequestParam LocalDate            startDate,
        @RequestParam LocalDate            endDate,
        @RequestParam Report.ReportFormat  format
    ) {
        Long requesterId = SecurityUtils.getCurrentUserId();
        byte[] data = reportService.generateRevenueReport(requesterId, startDate, endDate, format);
        return toDownload(data, "revenue_report", format);
    }

    @PostMapping("/occupancy")
    @Operation(summary = "Generate occupancy report")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER')")
    public ResponseEntity<byte[]> occupancyReport(
        @RequestParam Report.ReportFormat format
    ) {
        Long requesterId = SecurityUtils.getCurrentUserId();
        byte[] data = reportService.generateOccupancyReport(requesterId, format);
        return toDownload(data, "occupancy_report", format);
    }

    @PostMapping("/rent-collection")
    @Operation(summary = "Generate rent collection report")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER')")
    public ResponseEntity<byte[]> rentCollectionReport(
        @RequestParam int                  month,
        @RequestParam int                  year,
        @RequestParam Report.ReportFormat  format
    ) {
        Long requesterId = SecurityUtils.getCurrentUserId();
        byte[] data = reportService.generateRentCollectionReport(requesterId, month, year, format);
        return toDownload(data, "rent_collection_report", format);
    }

    @PostMapping("/tenant")
    @Operation(summary = "Generate tenant report")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER')")
    public ResponseEntity<byte[]> tenantReport(
        @RequestParam Report.ReportFormat format
    ) {
        Long requesterId = SecurityUtils.getCurrentUserId();
        byte[] data = reportService.generateTenantReport(requesterId, format);
        return toDownload(data, "tenant_report", format);
    }

    @PostMapping("/maintenance")
    @Operation(summary = "Generate maintenance report")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER')")
    public ResponseEntity<byte[]> maintenanceReport(
        @RequestParam LocalDate            startDate,
        @RequestParam LocalDate            endDate,
        @RequestParam Report.ReportFormat  format
    ) {
        Long requesterId = SecurityUtils.getCurrentUserId();
        byte[] data = reportService.generateMaintenanceReport(requesterId, startDate, endDate, format);
        return toDownload(data, "maintenance_report", format);
    }

    @GetMapping("/history")
    @Operation(summary = "Get report generation history")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER')")
    public ResponseEntity<PagedResponse<ReportResponse>> history(
        @RequestParam(defaultValue = "0")  int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(reportService.getHistory(userId, page, size));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private ResponseEntity<byte[]> toDownload(byte[] data, String baseName, Report.ReportFormat format) {
        String ext         = format == Report.ReportFormat.PDF ? ".pdf" : ".xlsx";
        String contentType = format == Report.ReportFormat.PDF
            ? "application/pdf"
            : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + baseName + ext)
            .contentType(MediaType.parseMediaType(contentType))
            .body(data);
    }
}
