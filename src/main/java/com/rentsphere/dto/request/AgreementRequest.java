package com.rentsphere.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class AgreementRequest {
    @NotNull private Long propertyId;
    @NotNull private Long tenantId;
    @NotNull private LocalDate startDate;
    @NotNull private LocalDate endDate;
    @NotNull @DecimalMin("0.01") private BigDecimal monthlyRent;
    @NotNull @DecimalMin("0.00") private BigDecimal securityDeposit;
}
