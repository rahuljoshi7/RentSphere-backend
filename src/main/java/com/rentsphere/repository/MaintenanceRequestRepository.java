package com.rentsphere.repository;

import com.rentsphere.entity.MaintenanceRequest;
import com.rentsphere.entity.MaintenanceRequest.MaintenanceStatus;
import com.rentsphere.entity.MaintenanceRequest.Priority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface MaintenanceRequestRepository extends JpaRepository<MaintenanceRequest, Long> {

    Page<MaintenanceRequest> findByTenantId(Long tenantId, Pageable pageable);

    Page<MaintenanceRequest> findByPropertyId(Long propertyId, Pageable pageable);

    Page<MaintenanceRequest> findByStatus(MaintenanceStatus status, Pageable pageable);

    @Query("""
        SELECT mr FROM MaintenanceRequest mr
        JOIN mr.assignments a
        WHERE a.staff.id = :staffId
        ORDER BY mr.createdAt DESC
        """)
    Page<MaintenanceRequest> findAssignedToStaff(@Param("staffId") Long staffId, Pageable pageable);

    @Query("""
        SELECT mr FROM MaintenanceRequest mr
        WHERE mr.property.owner.id = :ownerId
        ORDER BY mr.createdAt DESC
        """)
    Page<MaintenanceRequest> findByOwnerId(@Param("ownerId") Long ownerId, Pageable pageable);

    long countByStatus(MaintenanceStatus status);

    @Query("SELECT COUNT(mr) FROM MaintenanceRequest mr WHERE mr.property.id = :propertyId AND mr.status <> 'COMPLETED'")
    long countOpenByPropertyId(@Param("propertyId") Long propertyId);

    // ── Phase 7: Analytics ───────────────────────────────────────────────────

    long countByPriority(Priority priority);

    @Query("SELECT COUNT(mr) FROM MaintenanceRequest mr WHERE mr.property.owner.id = :ownerId AND mr.status <> 'COMPLETED'")
    long countOpenByOwnerId(@Param("ownerId") Long ownerId);

    @Query("SELECT COUNT(mr) FROM MaintenanceRequest mr WHERE mr.property.manager.id = :managerId AND mr.status <> 'COMPLETED'")
    long countOpenByManagerId(@Param("managerId") Long managerId);

    @Query("""
        SELECT COUNT(DISTINCT mr) FROM MaintenanceRequest mr
        JOIN mr.assignments a
        WHERE a.staff.id = :staffId
        """)
    long countAssignedToStaff(@Param("staffId") Long staffId);

    @Query("""
        SELECT COUNT(DISTINCT mr) FROM MaintenanceRequest mr
        JOIN mr.assignments a
        WHERE a.staff.id = :staffId AND mr.status = :status
        """)
    long countAssignedToStaffByStatus(@Param("staffId") Long staffId, @Param("status") MaintenanceStatus status);

    @Query("""
        SELECT COUNT(DISTINCT mr) FROM MaintenanceRequest mr
        JOIN mr.assignments a
        WHERE a.staff.id = :staffId AND mr.priority = :priority
        """)
    long countAssignedToStaffByPriority(@Param("staffId") Long staffId, @Param("priority") Priority priority);

    long countByTenantId(Long tenantId);

    long countByTenantIdAndStatusNot(Long tenantId, MaintenanceStatus status);

    // ── Report-optimised queries (replaces findAll() in ReportService) ─────────

    /**
     * Platform-wide: all requests created within [from, to] — eager-fetches property and tenant
     * so the report builder doesn't trigger N+1 selects.
     */
    @Query("""
        SELECT mr FROM MaintenanceRequest mr
        JOIN FETCH mr.property p
        JOIN FETCH p.owner
        JOIN FETCH mr.tenant t
        JOIN FETCH t.user
        WHERE mr.createdAt >= :from AND mr.createdAt < :to
        ORDER BY mr.createdAt DESC
        """)
    List<MaintenanceRequest> findByCreatedAtBetween(
        @Param("from") Instant from,
        @Param("to")   Instant to
    );

    /**
     * Owner-scoped: requests for properties owned by the given user, within [from, to).
     */
    @Query("""
        SELECT mr FROM MaintenanceRequest mr
        JOIN FETCH mr.property p
        JOIN FETCH p.owner
        JOIN FETCH mr.tenant t
        JOIN FETCH t.user
        WHERE p.owner.id = :ownerId
          AND mr.createdAt >= :from AND mr.createdAt < :to
        ORDER BY mr.createdAt DESC
        """)
    List<MaintenanceRequest> findByOwnerAndCreatedAtBetween(
        @Param("ownerId") Long ownerId,
        @Param("from")    Instant from,
        @Param("to")      Instant to
    );
}
