package com.rentsphere.repository;

import com.rentsphere.entity.Property;
import com.rentsphere.entity.Property.AvailabilityStatus;
import com.rentsphere.entity.Property.PropertyType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PropertyRepository extends JpaRepository<Property, Long>,
        JpaSpecificationExecutor<Property> {

    // ==================================================
    // BASIC QUERIES
    // ==================================================

    Page<Property> findByOwnerId(Long ownerId, Pageable pageable);

    Page<Property> findByManagerId(Long managerId, Pageable pageable);

    Page<Property> findByAvailabilityStatus(
            AvailabilityStatus status,
            Pageable pageable
    );

    // ==================================================
    // SEARCH & FILTER
    // ==================================================

    @Query("""
        SELECT p FROM Property p
        WHERE (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')))
          AND (:city IS NULL OR LOWER(p.city) LIKE LOWER(CONCAT('%', :city, '%')))
          AND (:type IS NULL OR p.propertyType = :type)
          AND (:status IS NULL OR p.availabilityStatus = :status)
          AND (:minRent IS NULL OR p.rentAmount >= :minRent)
          AND (:maxRent IS NULL OR p.rentAmount <= :maxRent)
        """)
    Page<Property> searchAndFilter(
            @Param("name") String name,
            @Param("city") String city,
            @Param("type") PropertyType type,
            @Param("status") AvailabilityStatus status,
            @Param("minRent") BigDecimal minRent,
            @Param("maxRent") BigDecimal maxRent,
            Pageable pageable
    );

    // ==================================================
    // DASHBOARD COUNTS
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
    // CITY LIST
    // ==================================================

    @Query("""
        SELECT DISTINCT p.city
        FROM Property p
        ORDER BY p.city
        """)
    List<String> findAllCities();
}
