package com.rentsphere.dto.response;

import com.rentsphere.entity.Payment;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data @Builder
public class PaymentResponse {
    private Long id;
    private Long agreementId;
    private Long tenantId;
    private String tenantName;
    private String propertyName;
    private int month;
    private int year;
    private BigDecimal amountDue;
    private BigDecimal amountPaid;
    private BigDecimal balance;
    private Payment.PaymentStatus status;
    private LocalDate dueDate;
    private LocalDate paidDate;
    private String notes;
    private Instant createdAt;
}
