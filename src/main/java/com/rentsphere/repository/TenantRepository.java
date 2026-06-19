package com.rentsphere.repository;

import com.rentsphere.entity.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {

    Optional<Tenant> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    @Query("""
        SELECT t FROM Tenant t
        JOIN t.user u
        WHERE LOWER(u.firstName) LIKE LOWER(CONCAT('%', :q, '%'))
           OR LOWER(u.lastName)  LIKE LOWER(CONCAT('%', :q, '%'))
           OR LOWER(u.email)     LIKE LOWER(CONCAT('%', :q, '%'))
           OR u.phone LIKE CONCAT('%', :q, '%')
        """)
    Page<Tenant> search(@Param("q") String query, Pageable pageable);

    @Query("""
        SELECT t FROM Tenant t
        JOIN t.agreements a
        WHERE a.property.id = :propertyId AND a.status = 'ACTIVE'
        """)
    Optional<Tenant> findActiveByPropertyId(@Param("propertyId") Long propertyId);

    @Query("""
        SELECT t FROM Tenant t
        JOIN t.agreements a
        WHERE a.property.owner.id = :ownerId AND a.status = 'ACTIVE'
        """)
    Page<Tenant> findActiveByOwnerId(@Param("ownerId") Long ownerId, Pageable pageable);

    @Query("SELECT COUNT(DISTINCT t.id) FROM Tenant t JOIN t.agreements a WHERE a.status = 'ACTIVE'")
    long countActive();

    @Query("""
        SELECT COUNT(DISTINCT t.id) FROM Tenant t
        JOIN t.agreements a
        WHERE a.property.owner.id = :ownerId AND a.status = 'ACTIVE'
        """)
    long countActiveByOwnerId(@Param("ownerId") Long ownerId);

    // ── Phase 7: Manager analytics ───────────────────────────────────────────

    @Query("""
        SELECT COUNT(DISTINCT t.id) FROM Tenant t
        JOIN t.agreements a
        WHERE a.property.manager.id = :managerId AND a.status = 'ACTIVE'
        """)
    long countActiveByManagerId(@Param("managerId") Long managerId);
}
