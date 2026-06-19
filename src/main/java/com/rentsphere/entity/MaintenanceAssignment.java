package com.rentsphere.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
    name = "maintenance_assignments",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_assignment_request_staff",
            columnNames = {"request_id", "staff_id"}
        )
    },
    indexes = {
        @Index(name = "idx_maint_assign_request", columnList = "request_id"),
        @Index(name = "idx_maint_assign_staff",   columnList = "staff_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private MaintenanceRequest request;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_id", nullable = false)
    private User staff;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist
    public void prePersist() {
        this.assignedAt = Instant.now();
    }

    public void markCompleted() {
        this.completedAt = Instant.now();
    }

    public boolean isCompleted() {
        return completedAt != null;
    }
}
