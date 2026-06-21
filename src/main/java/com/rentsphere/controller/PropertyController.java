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
@Tag(name = "Properties", description = "Property Management APIs")
@SecurityRequirement(name = "bearerAuth")
public class PropertyController {

    private final PropertyService propertyService;

    // ==================================================
    // CREATE PROPERTY
    // ==================================================

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER')")
    @Operation(summary = "Create Property")
    public ResponseEntity<PropertyResponse> createProperty(
            @Valid @RequestBody PropertyRequest request) {

        Long ownerId = SecurityUtils.getCurrentUserId();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(propertyService.create(request, ownerId));
    }

    // ==================================================
    // UPDATE PROPERTY
    // ==================================================

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER','PROPERTY_MANAGER')")
    @Operation(summary = "Update Property")
    public ResponseEntity<PropertyResponse> updateProperty(
            @PathVariable Long id,
            @Valid @RequestBody PropertyRequest request) {

        Long userId = SecurityUtils.getCurrentUserId();

        return ResponseEntity.ok(
                propertyService.update(id, request, userId)
        );
    }

    // ==================================================
    // DELETE PROPERTY
    // ==================================================

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER')")
    @Operation(summary = "Delete Property")
    public ResponseEntity<ApiResponse> deleteProperty(
            @PathVariable Long id) {

        Long userId = SecurityUtils.getCurrentUserId();

        propertyService.delete(id, userId);

        return ResponseEntity.ok(
                ApiResponse.success("Property deleted successfully.")
        );
    }

    // ==================================================
    // GET PROPERTY BY ID
    // ==================================================

    @GetMapping("/{id}")
    @Operation(summary = "Get Property By ID")
    public ResponseEntity<PropertyResponse> getProperty(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                propertyService.findById(id)
        );
    }

    // ==================================================
    // GET ALL PROPERTIES (ADMIN/OWNER/MANAGER)
    // ==================================================

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER','PROPERTY_MANAGER')")
    @Operation(summary = "Get All Properties")
    public ResponseEntity<PagedResponse<PropertyResponse>> getAllProperties(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(
                propertyService.findAll(page, size)
        );
    }

    // ==================================================
    // MY PROPERTIES
    // ==================================================

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('PROPERTY_OWNER','ADMIN')")
    @Operation(summary = "Get Logged-in Owner Properties")
    public ResponseEntity<PagedResponse<PropertyResponse>> getMyProperties(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Long ownerId = SecurityUtils.getCurrentUserId();

        return ResponseEntity.ok(
                propertyService.findByOwner(ownerId, page, size)
        );
    }

    // ==================================================
    // SEARCH PROPERTIES (PUBLIC)
    // ==================================================

    @GetMapping("/search")
    @Operation(summary = "Search Properties")
    public ResponseEntity<PagedResponse<PropertyResponse>> searchProperties(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Property.PropertyType type,
            @RequestParam(required = false) Property.AvailabilityStatus status,
            @RequestParam(required = false) BigDecimal minRent,
            @RequestParam(required = false) BigDecimal maxRent,
            @RequestParam(defaultValue = "newest") String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(
                propertyService.search(
                        name,
                        city,
                        type,
                        status,
                        minRent,
                        maxRent,
                        sortBy,
                        page,
                        size
                )
        );
    }

    // ==================================================
    // GET CITIES
    // ==================================================

    @GetMapping("/cities")
    @Operation(summary = "Get All Cities")
    public ResponseEntity<List<String>> getCities() {

        return ResponseEntity.ok(
                propertyService.getAllCities()
        );
    }

    // ==================================================
    // UPLOAD PROPERTY IMAGES
    // ==================================================

    @PostMapping(
            value = "/{id}/images",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER','PROPERTY_MANAGER')")
    @Operation(summary = "Upload Property Images")
    public ResponseEntity<PropertyResponse> uploadImages(
            @PathVariable Long id,
            @RequestPart("files") List<MultipartFile> files) {

        Long userId = SecurityUtils.getCurrentUserId();

        return ResponseEntity.ok(
                propertyService.uploadImages(id, files, userId)
        );
    }

    // ==================================================
    // DELETE PROPERTY IMAGE
    // ==================================================

    @DeleteMapping("/{id}/images/{imageId}")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER','PROPERTY_MANAGER')")
    @Operation(summary = "Delete Property Image")
    public ResponseEntity<ApiResponse> deleteImage(
            @PathVariable Long id,
            @PathVariable Long imageId) {

        Long userId = SecurityUtils.getCurrentUserId();

        propertyService.deleteImage(id, imageId, userId);

        return ResponseEntity.ok(
                ApiResponse.success("Image deleted successfully.")
        );
    }

    // ==================================================
    // UPDATE PROPERTY STATUS
    // ==================================================

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER','PROPERTY_MANAGER')")
    @Operation(summary = "Update Property Status")
    public ResponseEntity<PropertyResponse> updatePropertyStatus(
            @PathVariable Long id,
            @RequestParam Property.AvailabilityStatus status) {

        Long userId = SecurityUtils.getCurrentUserId();

        return ResponseEntity.ok(
                propertyService.updateStatus(id, status, userId)
        );
    }
}
