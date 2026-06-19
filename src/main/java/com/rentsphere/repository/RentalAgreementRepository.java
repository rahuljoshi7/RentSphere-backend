package com.rentsphere.repository;

import com.rentsphere.entity.RentalAgreement;
import com.rentsphere.entity.RentalAgreement.AgreementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RentalAgreementRepository extends JpaRepository<RentalAgreement, Long> {

    Page<RentalAgreement> findByTenantId(Long tenantId, Pageable pageable);

    Page<RentalAgreement> findByPropertyId(Long propertyId, Pageable pageable);

    Optional<RentalAgreement> findByPropertyIdAndStatus(Long propertyId, AgreementStatus status);

    Optional<RentalAgreement> findByTenantIdAndStatus(Long tenantId, AgreementStatus status);

    @Query("SELECT a FROM RentalAgreement a WHERE a.property.owner.id = :ownerId")
    Page<RentalAgreement> findByOwnerId(@Param("ownerId") Long ownerId, Pageable pageable);

    /**
     * Agreements expiring within the next {@code days} days — used by the scheduler.
     */
    @Query("""
        SELECT a FROM RentalAgreement a
        WHERE a.status = 'ACTIVE'
          AND a.endDate BETWEEN :today AND :cutoff
        """)
    List<RentalAgreement> findExpiringSoon(
        @Param("today")  LocalDate today,
        @Param("cutoff") LocalDate cutoff
    );

    @Query("""
        SELECT a FROM RentalAgreement a
        WHERE a.status = 'ACTIVE' AND a.endDate < :today
        """)
    List<RentalAgreement> findExpired(@Param("today") LocalDate today);

    boolean existsByPropertyIdAndStatus(Long propertyId, AgreementStatus status);

    @Query("SELECT COUNT(a) FROM RentalAgreement a WHERE a.status = 'ACTIVE'")
    long countActive();

    // ── Phase 7: Analytics ───────────────────────────────────────────────────

    @Query("""
        SELECT COUNT(a) FROM RentalAgreement a
        WHERE a.status = 'ACTIVE' AND a.endDate BETWEEN :today AND :cutoff
        """)
    long countExpiringSoon(@Param("today") LocalDate today, @Param("cutoff") LocalDate cutoff);

    @Query("""
        SELECT COUNT(a) FROM RentalAgreement a
        WHERE a.status = 'ACTIVE' AND a.property.owner.id = :ownerId
          AND a.endDate BETWEEN :today AND :cutoff
        """)
    long countExpiringSoonByOwnerId(
        @Param("ownerId") Long ownerId,
        @Param("today") LocalDate today,
        @Param("cutoff") LocalDate cutoff
    );

    @Query("SELECT COUNT(a) FROM RentalAgreement a WHERE a.status = 'ACTIVE' AND a.property.manager.id = :managerId")
    long countActiveByManagerId(@Param("managerId") Long managerId);
}
