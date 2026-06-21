package com.rentsphere.controller;

import com.rentsphere.dto.request.AgreementRequest;
import com.rentsphere.dto.response.AgreementResponse;
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
@Tag(name = "Rental Agreements", description = "Rental Agreement Management APIs")
@SecurityRequirement(name = "bearerAuth")
public class RentalAgreementController {

    private final RentalAgreementService agreementService;

    // ==================================================
    // CREATE AGREEMENT
    // ==================================================

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER','PROPERTY_MANAGER')")
    @Operation(summary = "Create Rental Agreement")
    public ResponseEntity<AgreementResponse> createAgreement(
            @Valid @RequestBody AgreementRequest request) {

        Long userId = SecurityUtils.getCurrentUserId();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(agreementService.create(request, userId));
    }

    // ==================================================
    // GET AGREEMENT BY ID
    // ==================================================

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER','PROPERTY_MANAGER','TENANT')")
    @Operation(summary = "Get Agreement By ID")
    public ResponseEntity<AgreementResponse> getAgreement(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                agreementService.findById(id)
        );
    }

    // ==================================================
    // PROPERTY AGREEMENTS
    // ==================================================

    @GetMapping("/property/{propertyId}")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER','PROPERTY_MANAGER')")
    @Operation(summary = "Get Agreements By Property")
    public ResponseEntity<PagedResponse<AgreementResponse>> getPropertyAgreements(
            @PathVariable Long propertyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(
                agreementService.findByProperty(propertyId, page, size)
        );
    }

    // ==================================================
    // TENANT AGREEMENTS (ADMIN / OWNER / MANAGER)
    // ==================================================

    @GetMapping("/tenant/{tenantId}")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER','PROPERTY_MANAGER')")
    @Operation(summary = "Get Agreements By Tenant")
    public ResponseEntity<PagedResponse<AgreementResponse>> getTenantAgreements(
            @PathVariable Long tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(
                agreementService.findByTenant(tenantId, page, size)
        );
    }

    // ==================================================
    // CURRENT TENANT AGREEMENTS
    // ==================================================

    @GetMapping("/my")
    @PreAuthorize("hasRole('TENANT')")
    @Operation(summary = "Get Logged-in Tenant Agreements")
    public ResponseEntity<PagedResponse<AgreementResponse>> getMyAgreements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Long tenantId = SecurityUtils.getCurrentUserId();

        return ResponseEntity.ok(
                agreementService.findByTenant(tenantId, page, size)
        );
    }

    // ==================================================
    // CURRENT OWNER AGREEMENTS
    // ==================================================

    @GetMapping("/owner/my")
    @PreAuthorize("hasAnyRole('PROPERTY_OWNER','ADMIN')")
    @Operation(summary = "Get Logged-in Owner Agreements")
    public ResponseEntity<PagedResponse<AgreementResponse>> getOwnerAgreements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Long ownerId = SecurityUtils.getCurrentUserId();

        return ResponseEntity.ok(
                agreementService.findByOwner(ownerId, page, size)
        );
    }

    // ==================================================
    // UPLOAD AGREEMENT DOCUMENT
    // ==================================================

    @PostMapping(
            value = "/{id}/document",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER','PROPERTY_MANAGER')")
    @Operation(summary = "Upload Agreement PDF")
    public ResponseEntity<AgreementResponse> uploadDocument(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file) {

        Long userId = SecurityUtils.getCurrentUserId();

        return ResponseEntity.ok(
                agreementService.uploadDocument(id, file, userId)
        );
    }

    // ==================================================
    // TERMINATE AGREEMENT
    // ==================================================

    @PatchMapping("/{id}/terminate")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER','PROPERTY_MANAGER')")
    @Operation(summary = "Terminate Agreement")
    public ResponseEntity<AgreementResponse> terminateAgreement(
            @PathVariable Long id) {

        Long userId = SecurityUtils.getCurrentUserId();

        return ResponseEntity.ok(
                agreementService.terminate(id, userId)
        );
    }

    // ==================================================
    // RENEW AGREEMENT
    // ==================================================

    @PostMapping("/{id}/renew")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER','PROPERTY_MANAGER')")
    @Operation(summary = "Renew Agreement")
    public ResponseEntity<AgreementResponse> renewAgreement(
            @PathVariable Long id,
            @Valid @RequestBody AgreementRequest request) {

        Long userId = SecurityUtils.getCurrentUserId();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(agreementService.renew(id, request, userId));
    }
}
