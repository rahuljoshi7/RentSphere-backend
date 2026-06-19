package com.rentsphere.controller;

import com.rentsphere.dto.request.AgreementRequest;
import com.rentsphere.dto.response.AgreementResponse;
import com.rentsphere.dto.response.ApiResponse;
import com.rentsphere.dto.response.PagedResponse;
import com.rentsphere.service.RentalAgreementService;
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
@RequestMapping("/api/v1/agreements")
@RequiredArgsConstructor
@Tag(name = "Rental Agreements", description = "Agreement lifecycle management")
@SecurityRequirement(name = "bearerAuth")
public class RentalAgreementController {

    private final RentalAgreementService agreementService;

    @PostMapping
    @Operation(summary = "Create a new rental agreement")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER','PROPERTY_MANAGER')")
    public ResponseEntity<AgreementResponse> create(@Valid @RequestBody AgreementRequest request) {
        Long requesterId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(agreementService.create(request, requesterId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get agreement by ID")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER','PROPERTY_MANAGER','TENANT')")
    public ResponseEntity<AgreementResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(agreementService.findById(id));
    }

    @GetMapping("/property/{propertyId}")
    @Operation(summary = "Get agreements by property")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER','PROPERTY_MANAGER')")
    public ResponseEntity<PagedResponse<AgreementResponse>> findByProperty(
        @PathVariable Long propertyId,
        @RequestParam(defaultValue = "0")  int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(agreementService.findByProperty(propertyId, page, size));
    }

    @GetMapping("/tenant/{tenantId}")
    @Operation(summary = "Get agreements by tenant")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER','PROPERTY_MANAGER','TENANT')")
    public ResponseEntity<PagedResponse<AgreementResponse>> findByTenant(
        @PathVariable Long tenantId,
        @RequestParam(defaultValue = "0")  int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(agreementService.findByTenant(tenantId, page, size));
    }

    @GetMapping("/my")
    @Operation(summary = "Get agreements for the authenticated owner")
    @PreAuthorize("hasAnyRole('PROPERTY_OWNER','ADMIN')")
    public ResponseEntity<PagedResponse<AgreementResponse>> findMine(
        @RequestParam(defaultValue = "0")  int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        Long ownerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(agreementService.findByOwner(ownerId, page, size));
    }

    @PostMapping(value = "/{id}/document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload agreement document (PDF)")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER','PROPERTY_MANAGER')")
    public ResponseEntity<AgreementResponse> uploadDocument(
        @PathVariable Long id,
        @RequestPart("file") MultipartFile file
    ) {
        Long requesterId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(agreementService.uploadDocument(id, file, requesterId));
    }

    @PatchMapping("/{id}/terminate")
    @Operation(summary = "Terminate an active agreement")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER','PROPERTY_MANAGER')")
    public ResponseEntity<AgreementResponse> terminate(@PathVariable Long id) {
        Long requesterId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(agreementService.terminate(id, requesterId));
    }

    @PostMapping("/{id}/renew")
    @Operation(summary = "Renew an agreement")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER','PROPERTY_MANAGER')")
    public ResponseEntity<AgreementResponse> renew(
        @PathVariable Long id,
        @Valid @RequestBody AgreementRequest request
    ) {
        Long requesterId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(agreementService.renew(id, request, requesterId));
    }
}
