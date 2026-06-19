package com.rentsphere.controller;

import com.rentsphere.dto.request.TenantProfileRequest;
import com.rentsphere.dto.response.ApiResponse;
import com.rentsphere.dto.response.PagedResponse;
import com.rentsphere.dto.response.TenantResponse;
import com.rentsphere.service.TenantService;
import com.rentsphere.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
@Tag(name = "Tenants", description = "Tenant management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class TenantController {

    private final TenantService tenantService;

    @GetMapping
    @Operation(summary = "List all tenants (Admin / Owner)")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER','PROPERTY_MANAGER')")
    public ResponseEntity<PagedResponse<TenantResponse>> findAll(
        @RequestParam(defaultValue = "0")  int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(tenantService.findAll(page, size));
    }

    @GetMapping("/search")
    @Operation(summary = "Search tenants by name, email, or phone")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER','PROPERTY_MANAGER')")
    public ResponseEntity<PagedResponse<TenantResponse>> search(
        @RequestParam String q,
        @RequestParam(defaultValue = "0")  int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(tenantService.search(q, page, size));
    }

    @GetMapping("/my-owner")
    @Operation(summary = "Get tenants under the authenticated owner")
    @PreAuthorize("hasRole('PROPERTY_OWNER')")
    public ResponseEntity<PagedResponse<TenantResponse>> findForOwner(
        @RequestParam(defaultValue = "0")  int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        Long ownerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(tenantService.findActiveByOwner(ownerId, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get tenant by ID")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER','PROPERTY_MANAGER')")
    public ResponseEntity<TenantResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(tenantService.findById(id));
    }

    @GetMapping("/me")
    @Operation(summary = "Get my tenant profile")
    @PreAuthorize("hasRole('TENANT')")
    public ResponseEntity<TenantResponse> getMyProfile() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(tenantService.findByUserId(userId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update tenant profile")
    @PreAuthorize("hasAnyRole('TENANT','ADMIN')")
    public ResponseEntity<TenantResponse> updateProfile(
        @PathVariable Long id,
        @Valid @RequestBody TenantProfileRequest request
    ) {
        Long requesterId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(tenantService.updateProfile(id, request, requesterId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate tenant (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> deactivate(@PathVariable Long id) {
        tenantService.deactivate(id, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("Tenant deactivated."));
    }
}
