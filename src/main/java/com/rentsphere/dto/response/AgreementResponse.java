package com.rentsphere.dto.response;

import com.rentsphere.entity.RentalAgreement;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data @Builder
public class AgreementResponse {
    private Long id;
    private Long propertyId;
    private String propertyName;
    private Long tenantId;
    private String tenantName;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal monthlyRent;
    private BigDecimal securityDeposit;
    private RentalAgreement.AgreementStatus status;
    private String documentUrl;
    private Instant createdAt;
    private Instant updatedAt;
}
