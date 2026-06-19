package com.rentsphere.controller;

import com.rentsphere.dto.response.*;
import com.rentsphere.service.DashboardService;
import com.rentsphere.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Analytics and KPI endpoints")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/admin")
    @Operation(summary = "Admin dashboard — platform-wide KPIs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminDashboardResponse> adminDashboard() {
        return ResponseEntity.ok(dashboardService.getAdminDashboard());
    }

    @GetMapping("/admin/analytics")
    @Operation(summary = "Admin analytics — revenue trend, breakdowns, top properties")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminAnalyticsResponse> adminAnalytics() {
        return ResponseEntity.ok(dashboardService.getAdminAnalytics());
    }

    @GetMapping("/owner")
    @Operation(summary = "Owner dashboard — portfolio KPIs")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER')")
    public ResponseEntity<OwnerDashboardResponse> ownerDashboard() {
        Long ownerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(dashboardService.getOwnerDashboard(ownerId));
    }

    @GetMapping("/owner/analytics")
    @Operation(summary = "Owner analytics — revenue trend, breakdowns, top properties")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER')")
    public ResponseEntity<OwnerAnalyticsResponse> ownerAnalytics() {
        Long ownerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(dashboardService.getOwnerAnalytics(ownerId));
    }

    @GetMapping("/manager")
    @Operation(summary = "Property manager dashboard — managed portfolio KPIs")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_MANAGER')")
    public ResponseEntity<ManagerDashboardResponse> managerDashboard() {
        Long managerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(dashboardService.getManagerDashboard(managerId));
    }

    @GetMapping("/tenant")
    @Operation(summary = "Tenant dashboard — lease, payments and maintenance summary")
    @PreAuthorize("hasAnyRole('ADMIN','TENANT')")
    public ResponseEntity<TenantDashboardResponse> tenantDashboard() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(dashboardService.getTenantDashboard(userId));
    }

    @GetMapping("/staff")
    @Operation(summary = "Maintenance staff dashboard — assigned request summary")
    @PreAuthorize("hasAnyRole('ADMIN','MAINTENANCE_STAFF')")
    public ResponseEntity<StaffDashboardResponse> staffDashboard() {
        Long staffId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(dashboardService.getStaffDashboard(staffId));
    }
}
