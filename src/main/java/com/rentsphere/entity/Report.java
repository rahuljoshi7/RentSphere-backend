package com.rentsphere.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(
    name = "reports",
    indexes = {
        @Index(name = "idx_reports_generated_by", columnList = "generated_by"),
        @Index(name = "idx_reports_type",         columnList = "report_type")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report {

    // ── Enums ────────────────────────────────────────────────────────────────

    public enum ReportType {
        REVENUE, OCCUPANCY, RENT_COLLECTION, TENANT, MAINTENANCE
    }

    public enum ReportFormat {
        PDF, EXCEL
    }

    // ── Fields ───────────────────────────────────────────────────────────────

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "generated_by", nullable = false)
    private User generatedBy;

    @Column(name = "report_type", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private ReportType reportType;

    @Column(name = "format", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private ReportFormat format;

    @Column(name = "file_url", length = 512)
    private String fileUrl;

    /**
     * Stores dynamic filter criteria as JSON.
     * Example: {"startDate":"2024-01-01","endDate":"2024-12-31","city":"Pune"}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "filters", columnDefinition = "jsonb")
    private Map<String, Object> filters;

    @Column(name = "generated_at", nullable = false, updatable = false)
    private Instant generatedAt;

    @PrePersist
    public void prePersist() {
        this.generatedAt = Instant.now();
    }
}
