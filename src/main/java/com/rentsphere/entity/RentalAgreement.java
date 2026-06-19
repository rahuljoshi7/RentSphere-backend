package com.rentsphere.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "rental_agreements",
    indexes = {
        @Index(name = "idx_agreements_property", columnList = "property_id"),
        @Index(name = "idx_agreements_tenant",   columnList = "tenant_id"),
        @Index(name = "idx_agreements_status",   columnList = "status"),
        @Index(name = "idx_agreements_end_date", columnList = "end_date")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RentalAgreement {

    // ── Enum ─────────────────────────────────────────────────────────────────

    public enum AgreementStatus {
        ACTIVE, EXPIRED, TERMINATED, RENEWED
    }

    // ── Fields ───────────────────────────────────────────────────────────────

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "monthly_rent", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyRent;

    @Column(name = "security_deposit", nullable = false, precision = 12, scale = 2)
    private BigDecimal securityDeposit;

    @Column(name = "status", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AgreementStatus status = AgreementStatus.ACTIVE;

    @Column(name = "document_url", length = 512)
    private String documentUrl;

    @Column(name = "cloudinary_doc_id", length = 255)
    private String cloudinaryDocId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // ── Relationships ────────────────────────────────────────────────────────

    @OneToMany(mappedBy = "agreement", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Payment> payments = new ArrayList<>();

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    public boolean isActive() {
        return status == AgreementStatus.ACTIVE;
    }

    public boolean isExpiringSoon(int withinDays) {
        return isActive() && endDate.minusDays(withinDays).isBefore(LocalDate.now());
    }

    public boolean isExpired() {
        return endDate.isBefore(LocalDate.now()) || status == AgreementStatus.EXPIRED;
    }
}
