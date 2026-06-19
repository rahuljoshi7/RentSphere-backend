package com.rentsphere.controller;

import com.rentsphere.dto.request.MaintenanceRequest;
import com.rentsphere.dto.request.MaintenanceStatusRequest;
import com.rentsphere.dto.response.ApiResponse;
import com.rentsphere.dto.response.MaintenanceResponse;
import com.rentsphere.dto.response.PagedResponse;
import com.rentsphere.entity.MaintenanceRequest.MaintenanceStatus;
import com.rentsphere.service.MaintenanceService;
import com.rentsphere.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/maintenance")
@RequiredArgsConstructor
@Tag(name = "Maintenance", description = "Maintenance request lifecycle")
@SecurityRequirement(name = "bearerAuth")
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    @PostMapping
    @Operation(summary = "Raise a maintenance request (Tenant)")
    @PreAuthorize("hasRole('TENANT')")
    public ResponseEntity<MaintenanceResponse> create(@Valid @RequestBody MaintenanceRequest request) {
        Long tenantUserId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(maintenanceService.create(request, tenantUserId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get maintenance request by ID")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER','PROPERTY_MANAGER','TENANT','MAINTENANCE_STAFF')")
    public ResponseEntity<MaintenanceResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(maintenanceService.findById(id));
    }

    @GetMapping("/my")
    @Operation(summary = "Get my maintenance requests (Tenant)")
    @PreAuthorize("hasRole('TENANT')")
    public ResponseEntity<PagedResponse<MaintenanceResponse>> findMine(
        @RequestParam(defaultValue = "0")  int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        Long userId = SecurityUtils.getCurrentUserId();
        // Resolve tenantId from userId via service (handled inside)
        return ResponseEntity.ok(maintenanceService.findByTenant(userId, page, size));
    }

    @GetMapping("/property/{propertyId}")
    @Operation(summary = "Get requests for a property")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER','PROPERTY_MANAGER')")
    public ResponseEntity<PagedResponse<MaintenanceResponse>> findByProperty(
        @PathVariable Long propertyId,
        @RequestParam(defaultValue = "0")  int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(maintenanceService.findByProperty(propertyId, page, size));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Filter maintenance requests by status")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER','PROPERTY_MANAGER','MAINTENANCE_STAFF')")
    public ResponseEntity<PagedResponse<MaintenanceResponse>> findByStatus(
        @PathVariable MaintenanceStatus status,
        @RequestParam(defaultValue = "0")  int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(maintenanceService.findByStatus(status, page, size));
    }

    @GetMapping("/assigned")
    @Operation(summary = "Get requests assigned to the authenticated staff member")
    @PreAuthorize("hasRole('MAINTENANCE_STAFF')")
    public ResponseEntity<PagedResponse<MaintenanceResponse>> findAssignedToMe(
        @RequestParam(defaultValue = "0")  int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        Long staffId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(maintenanceService.findAssignedToStaff(staffId, page, size));
    }

    @GetMapping("/owner")
    @Operation(summary = "Get all maintenance requests for the authenticated owner's properties")
    @PreAuthorize("hasRole('PROPERTY_OWNER')")
    public ResponseEntity<PagedResponse<MaintenanceResponse>> findForOwner(
        @RequestParam(defaultValue = "0")  int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        Long ownerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(maintenanceService.findByOwner(ownerId, page, size));
    }

    @PostMapping("/{id}/assign")
    @Operation(summary = "Assign a maintenance request to staff")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER','PROPERTY_MANAGER')")
    public ResponseEntity<MaintenanceResponse> assign(
        @PathVariable Long id,
        @RequestParam Long staffId
    ) {
        Long requesterId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(maintenanceService.assignToStaff(id, staffId, requesterId));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update maintenance request status")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER','PROPERTY_MANAGER','MAINTENANCE_STAFF')")
    public ResponseEntity<MaintenanceResponse> updateStatus(
        @PathVariable Long id,
        @Valid @RequestBody MaintenanceStatusRequest request
    ) {
        Long requesterId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(maintenanceService.updateStatus(id, request, requesterId));
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload image for a maintenance request (Tenant)")
    @PreAuthorize("hasRole('TENANT')")
    public ResponseEntity<MaintenanceResponse> uploadImage(
        @PathVariable Long id,
        @RequestPart("file") MultipartFile file
    ) {
        Long tenantUserId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(maintenanceService.uploadImage(id, file, tenantUserId));
    }

    @PatchMapping("/{id}/close")
    @Operation(summary = "Close a completed maintenance request")
    @PreAuthorize("hasAnyRole('ADMIN','MAINTENANCE_STAFF','PROPERTY_OWNER','PROPERTY_MANAGER')")
    public ResponseEntity<ApiResponse> close(@PathVariable Long id) {
        Long staffId = SecurityUtils.getCurrentUserId();
        maintenanceService.close(id, staffId);
        return ResponseEntity.ok(ApiResponse.success("Request closed."));
    }
}
