package com.rentsphere.controller;

import com.rentsphere.dto.request.FacilityComplaintRequest;
import com.rentsphere.dto.request.FacilityRequest;
import com.rentsphere.dto.response.ApiResponse;
import com.rentsphere.dto.response.FacilityComplaintResponse;
import com.rentsphere.dto.response.FacilityResponse;
import com.rentsphere.dto.response.PagedResponse;
import com.rentsphere.entity.FacilityComplaint;
import com.rentsphere.service.FacilityService;
import com.rentsphere.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/facilities")
@RequiredArgsConstructor
@Tag(name = "Facilities", description = "Facility management and complaint tracking")
@SecurityRequirement(name = "bearerAuth")
public class FacilityController {

    private final FacilityService facilityService;

    @PostMapping("/property/{propertyId}")
    @Operation(summary = "Add a facility to a property")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER','PROPERTY_MANAGER')")
    public ResponseEntity<FacilityResponse> addFacility(
        @PathVariable Long propertyId,
        @Valid @RequestBody FacilityRequest request
    ) {
        Long requesterId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(facilityService.addFacility(propertyId, request, requesterId));
    }

    @PutMapping("/{facilityId}")
    @Operation(summary = "Update facility status or description")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER','PROPERTY_MANAGER')")
    public ResponseEntity<FacilityResponse> updateFacility(
        @PathVariable Long facilityId,
        @Valid @RequestBody FacilityRequest request
    ) {
        Long requesterId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(facilityService.updateFacility(facilityId, request, requesterId));
    }

    @GetMapping("/property/{propertyId}")
    @Operation(summary = "Get all facilities for a property")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER','PROPERTY_MANAGER','TENANT')")
    public ResponseEntity<List<FacilityResponse>> findByProperty(@PathVariable Long propertyId) {
        return ResponseEntity.ok(facilityService.findByProperty(propertyId));
    }

    // ── Complaints ────────────────────────────────────────────────────────────

    @PostMapping("/complaints")
    @Operation(summary = "Raise a facility complaint (Tenant)")
    @PreAuthorize("hasRole('TENANT')")
    public ResponseEntity<FacilityComplaintResponse> raiseComplaint(
        @Valid @RequestBody FacilityComplaintRequest request
    ) {
        Long tenantUserId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(facilityService.raiseComplaint(request, tenantUserId));
    }

    @PatchMapping("/complaints/{complaintId}/resolve")
    @Operation(summary = "Resolve a facility complaint")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER','PROPERTY_MANAGER')")
    public ResponseEntity<FacilityComplaintResponse> resolveComplaint(@PathVariable Long complaintId) {
        Long requesterId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(facilityService.resolveComplaint(complaintId, requesterId));
    }

    @GetMapping("/complaints/property/{propertyId}")
    @Operation(summary = "Get facility complaints for a property")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER','PROPERTY_MANAGER')")
    public ResponseEntity<PagedResponse<FacilityComplaintResponse>> findComplaintsByProperty(
        @PathVariable Long propertyId,
        @RequestParam(required = false, defaultValue = "OPEN") FacilityComplaint.ComplaintStatus status,
        @RequestParam(defaultValue = "0")  int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
            facilityService.findComplaintsByProperty(propertyId, status, page, size)
        );
    }

    @GetMapping("/complaints/my")
    @Operation(summary = "Get my facility complaints (Tenant)")
    @PreAuthorize("hasRole('TENANT')")
    public ResponseEntity<PagedResponse<FacilityComplaintResponse>> findMyComplaints(
        @RequestParam(defaultValue = "0")  int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        Long tenantId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(facilityService.findComplaintsByTenant(tenantId, page, size));
    }
}
