package com.rentsphere.controller;

import com.rentsphere.dto.request.PropertyRequest;
import com.rentsphere.dto.response.ApiResponse;
import com.rentsphere.dto.response.PagedResponse;
import com.rentsphere.dto.response.PropertyResponse;
import com.rentsphere.entity.Property;
import com.rentsphere.service.PropertyService;
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

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/properties")
@RequiredArgsConstructor
@Tag(name = "Properties", description = "Property management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class PropertyController {

    private final PropertyService propertyService;

    // ── Create ────────────────────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Create a new property")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER')")
    public ResponseEntity<PropertyResponse> create(@Valid @RequestBody PropertyRequest request) {
        Long ownerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(propertyService.create(request, ownerId));
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @PutMapping("/{id}")
    @Operation(summary = "Update property details")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER','PROPERTY_MANAGER')")
    public ResponseEntity<PropertyResponse> update(
        @PathVariable Long id,
        @Valid @RequestBody PropertyRequest request
    ) {
        Long requesterId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(propertyService.update(id, request, requesterId));
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a property")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER')")
    public ResponseEntity<ApiResponse> delete(@PathVariable Long id) {
        Long requesterId = SecurityUtils.getCurrentUserId();
        propertyService.delete(id, requesterId);
        return ResponseEntity.ok(ApiResponse.success("Property deleted successfully."));
    }

    // ── Get by ID ─────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    @Operation(summary = "Get property by ID")
    public ResponseEntity<PropertyResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(propertyService.findById(id));
    }

    // ── Get all ───────────────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "Get all properties (paginated)")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER','PROPERTY_MANAGER')")
    public ResponseEntity<PagedResponse<PropertyResponse>> findAll(
        @RequestParam(defaultValue = "0")  int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(propertyService.findAll(page, size));
    }

    // ── My properties (owner) ─────────────────────────────────────────────────

    @GetMapping("/my")
    @Operation(summary = "Get properties owned by the authenticated user")
    @PreAuthorize("hasAnyRole('PROPERTY_OWNER','ADMIN')")
    public ResponseEntity<PagedResponse<PropertyResponse>> findMine(
        @RequestParam(defaultValue = "0")  int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        Long ownerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(propertyService.findByOwner(ownerId, page, size));
    }

    // ── Search & Filter ───────────────────────────────────────────────────────

    @GetMapping("/search")
    @Operation(summary = "Search and filter properties (public)")
    public ResponseEntity<PagedResponse<PropertyResponse>> search(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String city,
        @RequestParam(required = false) Property.PropertyType type,
        @RequestParam(required = false) Property.AvailabilityStatus status,
        @RequestParam(required = false) BigDecimal minRent,
        @RequestParam(required = false) BigDecimal maxRent,
        @RequestParam(required = false, defaultValue = "newest") String sortBy,
        @RequestParam(defaultValue = "0")  int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
            propertyService.search(name, city, type, status, minRent, maxRent, sortBy, page, size)
        );
    }

    // ── Cities list ───────────────────────────────────────────────────────────

    @GetMapping("/cities")
    @Operation(summary = "Get distinct cities (public)")
    public ResponseEntity<List<String>> getCities() {
        return ResponseEntity.ok(propertyService.getAllCities());
    }

    // ── Upload images ─────────────────────────────────────────────────────────

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload images for a property")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER','PROPERTY_MANAGER')")
    public ResponseEntity<PropertyResponse> uploadImages(
        @PathVariable Long id,
        @RequestPart("files") List<MultipartFile> files
    ) {
        Long requesterId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(propertyService.uploadImages(id, files, requesterId));
    }

    // ── Delete image ──────────────────────────────────────────────────────────

    @DeleteMapping("/{id}/images/{imageId}")
    @Operation(summary = "Delete a property image")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER','PROPERTY_MANAGER')")
    public ResponseEntity<ApiResponse> deleteImage(
        @PathVariable Long id,
        @PathVariable Long imageId
    ) {
        Long requesterId = SecurityUtils.getCurrentUserId();
        propertyService.deleteImage(id, imageId, requesterId);
        return ResponseEntity.ok(ApiResponse.success("Image deleted successfully."));
    }

    // ── Update status ─────────────────────────────────────────────────────────

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update property availability status")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER','PROPERTY_MANAGER')")
    public ResponseEntity<PropertyResponse> updateStatus(
        @PathVariable Long id,
        @RequestParam Property.AvailabilityStatus status
    ) {
        Long requesterId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(propertyService.updateStatus(id, status, requesterId));
    }
}
