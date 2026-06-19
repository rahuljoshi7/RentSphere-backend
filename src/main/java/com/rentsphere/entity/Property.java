package com.rentsphere.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "properties",
    indexes = {
        @Index(name = "idx_properties_owner",     columnList = "owner_id"),
        @Index(name = "idx_properties_manager",   columnList = "manager_id"),
        @Index(name = "idx_properties_city",      columnList = "city"),
        @Index(name = "idx_properties_type",      columnList = "property_type"),
        @Index(name = "idx_properties_status",    columnList = "availability_status"),
        @Index(name = "idx_properties_rent",      columnList = "rent_amount"),
        @Index(name = "idx_properties_city_type", columnList = "city, property_type"),
        @Index(name = "idx_properties_created",   columnList = "created_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Property {

    // ── Enums ────────────────────────────────────────────────────────────────

    public enum PropertyType {
        APARTMENT, VILLA, HOUSE, COMMERCIAL, OFFICE, SHOP
    }

    public enum AvailabilityStatus {
        AVAILABLE, OCCUPIED, UNDER_MAINTENANCE
    }

    // ── Fields ───────────────────────────────────────────────────────────────

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private User manager;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "property_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private PropertyType propertyType;

    @Column(name = "address", nullable = false, columnDefinition = "TEXT")
    private String address;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "state", nullable = false, length = 100)
    private String state;

    @Column(name = "rent_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal rentAmount;

    @Column(name = "deposit_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal depositAmount;

    @Column(name = "num_rooms", nullable = false)
    private Integer numRooms;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "availability_status", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AvailabilityStatus availabilityStatus = AvailabilityStatus.AVAILABLE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // ── Relationships ────────────────────────────────────────────────────────

    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PropertyImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "property", fetch = FetchType.LAZY)
    @Builder.Default
    private List<RentalAgreement> agreements = new ArrayList<>();

    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<MaintenanceRequest> maintenanceRequests = new ArrayList<>();

    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Facility> facilities = new ArrayList<>();

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

    public void addImage(PropertyImage image) {
        images.add(image);
        image.setProperty(this);
    }

    public void removeImage(PropertyImage image) {
        images.remove(image);
        image.setProperty(null);
    }
}
