package com.rentsphere.repository;

import com.rentsphere.entity.Property;
import com.rentsphere.entity.Property.AvailabilityStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PropertyRepository extends JpaRepository<Property, Long>,
        JpaSpecificationExecutor<Property> {

    // ==================================================
    // PROPERTY LISTS
    // ==================================================

    Page<Property> findByOwnerId(
            Long ownerId,
            Pageable pageable
    );

    Page<Property> findByManagerId(
            Long managerId,
            Pageable pageable
    );

    Page<Property> findByAvailabilityStatus(
            AvailabilityStatus status,
            Pageable pageable
    );

    // ==================================================
    // DASHBOARD STATS
    // ==================================================

    long countByAvailabilityStatus(
            AvailabilityStatus status
    );

    long countByOwnerId(
            Long ownerId
    );

    long countByOwnerIdAndAvailabilityStatus(
            Long ownerId,
            AvailabilityStatus status
    );

    long countByManagerId(
            Long managerId
    );

    long countByManagerIdAndAvailabilityStatus(
            Long managerId,
            AvailabilityStatus status
    );

    // ==================================================
    // LEGACY METHODS USED IN DASHBOARDSERVICE
    // ==================================================

    @Query("""
        SELECT COUNT(p)
        FROM Property p
        WHERE p.availabilityStatus = 'OCCUPIED'
    """)
    long countOccupied();

    @Query("""
        SELECT COUNT(p)
        FROM Property p
        WHERE p.availabilityStatus = 'AVAILABLE'
    """)
    long countAvailable();

    @Query("""
        SELECT COUNT(p)
        FROM Property p
        WHERE p.owner.id = :ownerId
        AND p.availabilityStatus = 'OCCUPIED'
    """)
    long countOccupiedByOwnerId(
            @Param("ownerId") Long ownerId
    );

    @Query("""
        SELECT COUNT(p)
        FROM Property p
        WHERE p.manager.id = :managerId
        AND p.availabilityStatus = 'OCCUPIED'
    """)
    long countOccupiedByManagerId(
            @Param("managerId") Long managerId
    );

    // ==================================================
    // CITY LIST
    // ==================================================

    @Query("""
        SELECT DISTINCT p.city
        FROM Property p
        ORDER BY p.city
    """)
    List<String> findAllCities();
}
