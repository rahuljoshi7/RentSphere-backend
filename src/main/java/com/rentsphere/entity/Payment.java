package com.rentsphere.entity;

import jakarta.persistence.*;
import lombok.*;
// Property is in the same package — no import required

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
    name = "payments",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_payment_agreement_month_year",
            columnNames = {"agreement_id", "month", "year"}
        )
    },
    indexes = {
        @Index(name = "idx_payments_agreement",  columnList = "agreement_id"),
        @Index(name = "idx_payments_tenant",     columnList = "tenant_id"),
        @Index(name = "idx_payments_status",     columnList = "status"),
        @Index(name = "idx_payments_due_date",   columnList = "due_date"),
        @Index(name = "idx_payments_month_year", columnList = "year, month")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    // ── Enum ─────────────────────────────────────────────────────────────────

    public enum PaymentStatus {
        PENDING, PAID, OVERDUE
    }

    // ── Fields ───────────────────────────────────────────────────────────────

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agreement_id", nullable = false)
    private RentalAgreement agreement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "month", nullable = false)
    private Integer month;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "amount_due", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountDue;

    @Column(name = "amount_paid", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "paid_date")
    private LocalDate paidDate;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    public BigDecimal getBalance() {
        return amountDue.subtract(amountPaid);
    }

    public boolean isFullyPaid() {
        return amountPaid.compareTo(amountDue) >= 0;
    }

    public void markAsPaid(BigDecimal paidAmount) {
        this.amountPaid = paidAmount;
        this.paidDate   = LocalDate.now();
        this.status     = paidAmount.compareTo(amountDue) >= 0 ? PaymentStatus.PAID : PaymentStatus.PENDING;
    }

    public void checkAndMarkOverdue() {
        if (status == PaymentStatus.PENDING && dueDate.isBefore(LocalDate.now())) {
            this.status = PaymentStatus.OVERDUE;
        }
    }

    /** Convenience accessor — navigates through the agreement. */
    public Property getProperty() {
        return agreement != null ? agreement.getProperty() : null;
    }
}
