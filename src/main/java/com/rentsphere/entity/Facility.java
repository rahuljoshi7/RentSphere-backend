package com.rentsphere.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "facilities",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_facility_property_type",
            columnNames = {"property_id", "facility_type"}
        )
    },
    indexes = {
        @Index(name = "idx_facilities_property", columnList = "property_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Facility {

    // ── Enums ────────────────────────────────────────────────────────────────

    public enum FacilityType {
        PARKING, SECURITY, WATER_SUPPLY, ELECTRICITY, HOUSEKEEPING
    }

    public enum FacilityStatus {
        ACTIVE, INACTIVE, UNDER_REPAIR
    }

    // ── Fields ───────────────────────────────────────────────────────────────

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(name = "facility_type", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private FacilityType facilityType;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private FacilityStatus status = FacilityStatus.ACTIVE;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // ── Relationships ────────────────────────────────────────────────────────

    @OneToMany(mappedBy = "facility", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<FacilityComplaint> complaints = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
    }
}
