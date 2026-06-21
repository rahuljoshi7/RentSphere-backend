package com.rentsphere.controller;

import com.rentsphere.dto.request.PaymentRequest;
import com.rentsphere.dto.response.ApiResponse;
import com.rentsphere.dto.response.PagedResponse;
import com.rentsphere.dto.response.PaymentResponse;
import com.rentsphere.entity.Payment;
import com.rentsphere.service.PaymentService;
import com.rentsphere.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Rent Payment Management APIs")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    // ==================================================
    // GENERATE PAYMENT
    // ==================================================

    @PostMapping("/generate/{agreementId}")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER','PROPERTY_MANAGER')")
    @Operation(summary = "Generate Monthly Payment")
    public ResponseEntity<PaymentResponse> generatePayment(
            @PathVariable Long agreementId,
            @RequestParam @Min(1) @Max(12) int month,
            @RequestParam @Min(2020) int year) {

        Long userId = SecurityUtils.getCurrentUserId();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.generateMonthlyPayment(
                        agreementId,
                        month,
                        year,
                        userId
                ));
    }

    // ==================================================
    // RECORD PAYMENT
    // ==================================================

    @PatchMapping("/{id}/record")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER','PROPERTY_MANAGER')")
    @Operation(summary = "Record Payment")
    public ResponseEntity<PaymentResponse> recordPayment(
            @PathVariable Long id,
            @Valid @RequestBody PaymentRequest request) {

        Long userId = SecurityUtils.getCurrentUserId();

        return ResponseEntity.ok(
                paymentService.recordPayment(id, request, userId)
        );
    }

    // ==================================================
    // GET PAYMENT BY ID
    // ==================================================

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER','PROPERTY_MANAGER','TENANT')")
    @Operation(summary = "Get Payment By ID")
    public ResponseEntity<PaymentResponse> getPayment(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                paymentService.findById(id)
        );
    }

    // ==================================================
    // TENANT PAYMENT HISTORY
    // ==================================================

    @GetMapping("/tenant/{tenantId}")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER','PROPERTY_MANAGER')")
    @Operation(summary = "Get Payments By Tenant")
    public ResponseEntity<PagedResponse<PaymentResponse>> getTenantPayments(
            @PathVariable Long tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(
                paymentService.findByTenant(tenantId, page, size)
        );
    }

    // ==================================================
    // AGREEMENT PAYMENTS
    // ==================================================

    @GetMapping("/agreement/{agreementId}")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER','PROPERTY_MANAGER','TENANT')")
    @Operation(summary = "Get Payments By Agreement")
    public ResponseEntity<PagedResponse<PaymentResponse>> getAgreementPayments(
            @PathVariable Long agreementId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(
                paymentService.findByAgreement(agreementId, page, size)
        );
    }

    // ==================================================
    // CURRENT TENANT PAYMENTS
    // ==================================================

    @GetMapping("/my")
    @PreAuthorize("hasRole('TENANT')")
    @Operation(summary = "Get Logged-in Tenant Payments")
    public ResponseEntity<PagedResponse<PaymentResponse>> getMyPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Long tenantId = SecurityUtils.getCurrentUserId();

        return ResponseEntity.ok(
                paymentService.findByTenant(tenantId, page, size)
        );
    }

    // ==================================================
    // CURRENT OWNER PAYMENTS
    // ==================================================

    @GetMapping("/owner/my")
    @PreAuthorize("hasAnyRole('PROPERTY_OWNER','ADMIN')")
    @Operation(summary = "Get Logged-in Owner Payments")
    public ResponseEntity<PagedResponse<PaymentResponse>> getOwnerPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Long ownerId = SecurityUtils.getCurrentUserId();

        return ResponseEntity.ok(
                paymentService.findByOwner(ownerId, page, size)
        );
    }

    // ==================================================
    // PAYMENTS BY STATUS
    // ==================================================

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER','PROPERTY_MANAGER')")
    @Operation(summary = "Get Payments By Status")
    public ResponseEntity<PagedResponse<PaymentResponse>> getPaymentsByStatus(
            @PathVariable Payment.PaymentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(
                paymentService.findByStatus(status, page, size)
        );
    }

    // ==================================================
    // BULK PAYMENT GENERATION
    // ==================================================

    @PostMapping("/generate-bulk")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Generate Monthly Payments For All Agreements")
    public ResponseEntity<ApiResponse> generateBulkPayments(
            @RequestParam @Min(1) @Max(12) int month,
            @RequestParam @Min(2020) int year) {

        paymentService.generateMonthlyRentForAllActive(month, year);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Bulk payment generation triggered for "
                                + month + "/" + year
                )
        );
    }
}
