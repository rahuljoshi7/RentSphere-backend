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
    // BASIC PROPERTY QUERIES
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
        WHERE (:name IS NULL OR p.name LIKE CONCAT('%', :name, '%'))
          AND (:city IS NULL OR p.city LIKE CONCAT('%', :city, '%'))
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
    // DASHBOARD STATISTICS
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
        """)
    long countByOwnerId(@Param("ownerId") Long ownerId);

    @Query("""
        SELECT COUNT(p)
        FROM Property p
        WHERE p.owner.id = :ownerId
          AND p.availabilityStatus = 'OCCUPIED'
        """)
    long countOccupiedByOwnerId(@Param("ownerId") Long ownerId);

    @Query("""
        SELECT COUNT(p)
        FROM Property p
        WHERE p.manager.id = :managerId
        """)
    long countByManagerId(@Param("managerId") Long managerId);

    @Query("""
        SELECT COUNT(p)
        FROM Property p
        WHERE p.manager.id = :managerId
          AND p.availabilityStatus = 'OCCUPIED'
        """)
    long countOccupiedByManagerId(@Param("managerId") Long managerId);

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
