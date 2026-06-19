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
@Tag(name = "Payments", description = "Rent payment tracking and management")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/generate/{agreementId}")
    @Operation(summary = "Generate monthly payment for an agreement")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER','PROPERTY_MANAGER')")
    public ResponseEntity<PaymentResponse> generate(
        @PathVariable Long agreementId,
        @RequestParam @Min(1) @Max(12) int month,
        @RequestParam @Min(2020)       int year
    ) {
        Long requesterId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(paymentService.generateMonthlyPayment(agreementId, month, year, requesterId));
    }

    @PatchMapping("/{id}/record")
    @Operation(summary = "Record a payment (mark as paid)")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER','PROPERTY_MANAGER')")
    public ResponseEntity<PaymentResponse> record(
        @PathVariable Long id,
        @Valid @RequestBody PaymentRequest request
    ) {
        Long requesterId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(paymentService.recordPayment(id, request, requesterId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payment by ID")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER','PROPERTY_MANAGER','TENANT')")
    public ResponseEntity<PaymentResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.findById(id));
    }

    @GetMapping("/tenant/{tenantId}")
    @Operation(summary = "Get payment history for a tenant")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER','PROPERTY_MANAGER','TENANT')")
    public ResponseEntity<PagedResponse<PaymentResponse>> findByTenant(
        @PathVariable Long tenantId,
        @RequestParam(defaultValue = "0")  int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(paymentService.findByTenant(tenantId, page, size));
    }

    @GetMapping("/agreement/{agreementId}")
    @Operation(summary = "Get payments for an agreement")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER','PROPERTY_MANAGER','TENANT')")
    public ResponseEntity<PagedResponse<PaymentResponse>> findByAgreement(
        @PathVariable Long agreementId,
        @RequestParam(defaultValue = "0")  int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(paymentService.findByAgreement(agreementId, page, size));
    }

    @GetMapping("/my")
    @Operation(summary = "Get payments under the authenticated owner's properties")
    @PreAuthorize("hasAnyRole('PROPERTY_OWNER','ADMIN')")
    public ResponseEntity<PagedResponse<PaymentResponse>> findMine(
        @RequestParam(defaultValue = "0")  int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        Long ownerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(paymentService.findByOwner(ownerId, page, size));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get payments by status (PENDING / PAID / OVERDUE)")
    @PreAuthorize("hasAnyRole('ADMIN','PROPERTY_OWNER','PROPERTY_MANAGER')")
    public ResponseEntity<PagedResponse<PaymentResponse>> findByStatus(
        @PathVariable Payment.PaymentStatus status,
        @RequestParam(defaultValue = "0")  int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(paymentService.findByStatus(status, page, size));
    }

    @PostMapping("/generate-bulk")
    @Operation(summary = "Manually trigger bulk monthly rent generation (Admin)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> generateBulk(
        @RequestParam @Min(1) @Max(12) int month,
        @RequestParam @Min(2020)       int year
    ) {
        paymentService.generateMonthlyRentForAllActive(month, year);
        return ResponseEntity.ok(ApiResponse.success("Bulk payment generation triggered for " + month + "/" + year));
    }
}
