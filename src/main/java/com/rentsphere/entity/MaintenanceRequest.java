package com.rentsphere.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "maintenance_requests",
    indexes = {
        @Index(name = "idx_maint_req_property", columnList = "property_id"),
        @Index(name = "idx_maint_req_tenant",   columnList = "tenant_id"),
        @Index(name = "idx_maint_req_status",   columnList = "status"),
        @Index(name = "idx_maint_req_priority", columnList = "priority")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceRequest {

    // ── Enums ────────────────────────────────────────────────────────────────

    public enum MaintenanceStatus {
        OPEN, ASSIGNED, IN_PROGRESS, COMPLETED
    }

    public enum Priority {
        LOW, MEDIUM, HIGH, URGENT
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

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "status", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private MaintenanceStatus status = MaintenanceStatus.OPEN;

    @Column(name = "priority", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    // ── Relationships ────────────────────────────────────────────────────────

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<MaintenanceAssignment> assignments = new ArrayList<>();

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

    public void close() {
        this.status   = MaintenanceStatus.COMPLETED;
        this.closedAt = Instant.now();
    }

    public boolean isOpen() {
        return status == MaintenanceStatus.OPEN;
    }

    public boolean isCompleted() {
        return status == MaintenanceStatus.COMPLETED;
    }
}
