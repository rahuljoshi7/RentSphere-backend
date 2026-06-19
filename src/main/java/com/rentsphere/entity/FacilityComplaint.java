package com.rentsphere.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
    name = "facility_complaints",
    indexes = {
        @Index(name = "idx_fac_complaints_facility", columnList = "facility_id"),
        @Index(name = "idx_fac_complaints_tenant",   columnList = "tenant_id"),
        @Index(name = "idx_fac_complaints_status",   columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacilityComplaint {

    // ── Enum ─────────────────────────────────────────────────────────────────

    public enum ComplaintStatus {
        OPEN, IN_PROGRESS, RESOLVED
    }

    // ── Fields ───────────────────────────────────────────────────────────────

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "facility_id", nullable = false)
    private Facility facility;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "complaint_text", nullable = false, columnDefinition = "TEXT")
    private String complaintText;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ComplaintStatus status = ComplaintStatus.OPEN;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
    }

    public void resolve() {
        this.status     = ComplaintStatus.RESOLVED;
        this.resolvedAt = Instant.now();
    }
}
