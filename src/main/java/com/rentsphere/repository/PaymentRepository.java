package com.rentsphere.repository;

import com.rentsphere.entity.Payment;
import com.rentsphere.entity.Payment.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Page<Payment> findByTenantId(Long tenantId, Pageable pageable);

    Page<Payment> findByAgreementId(Long agreementId, Pageable pageable);

    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);

    Optional<Payment> findByAgreementIdAndMonthAndYear(Long agreementId, int month, int year);

    @Query("SELECT p FROM Payment p WHERE p.status IN ('PENDING','OVERDUE') AND p.dueDate < :today")
    List<Payment> findPendingOverdue(@Param("today") LocalDate today);

    @Query("SELECT p FROM Payment p WHERE p.status = 'PENDING' AND p.dueDate = :dueDate")
    List<Payment> findDueOn(@Param("dueDate") LocalDate dueDate);

    @Query("""
        SELECT p FROM Payment p
        WHERE p.agreement.property.owner.id = :ownerId
        ORDER BY p.createdAt DESC
        """)
    Page<Payment> findByOwnerId(@Param("ownerId") Long ownerId, Pageable pageable);

    /** Monthly revenue for a given owner — used by owner dashboard. */
    @Query("""
        SELECT COALESCE(SUM(p.amountPaid), 0)
        FROM Payment p
        WHERE p.status = 'PAID'
          AND p.agreement.property.owner.id = :ownerId
          AND p.year = :year
          AND p.month = :month
        """)
    BigDecimal sumPaidByOwnerAndMonth(
        @Param("ownerId") Long ownerId,
        @Param("year")    int year,
        @Param("month")   int month
    );

    /** Total platform revenue for admin dashboard. */
    @Query("""
        SELECT COALESCE(SUM(p.amountPaid), 0)
        FROM Payment p
        WHERE p.status = 'PAID' AND p.year = :year AND p.month = :month
        """)
    BigDecimal sumPaidByMonth(@Param("year") int year, @Param("month") int month);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = 'PENDING' OR p.status = 'OVERDUE'")
    long countPending();

    @Query("""
        SELECT COALESCE(SUM(p.amountPaid), 0)
        FROM Payment p
        WHERE p.status = 'PAID'
          AND p.agreement.property.owner.id = :ownerId
          AND p.year = :year
        """)
    BigDecimal sumPaidByOwnerAndYear(@Param("ownerId") Long ownerId, @Param("year") int year);

    // ── Phase 7: Analytics ──────────────────────────────────────────────────

    /** Platform-wide monthly revenue across a year span — used to build the trend chart. */
    @Query("""
        SELECT p.year as year, p.month as month, COALESCE(SUM(p.amountPaid), 0) as revenue
        FROM Payment p
        WHERE p.status = 'PAID'
          AND ((p.year = :startYear AND p.month >= :startMonth) OR p.year > :startYear)
          AND ((p.year = :endYear AND p.month <= :endMonth) OR p.year < :endYear)
        GROUP BY p.year, p.month
        ORDER BY p.year, p.month
        """)
    List<Object[]> sumPaidGroupedByMonth(
        @Param("startYear") int startYear, @Param("startMonth") int startMonth,
        @Param("endYear") int endYear, @Param("endMonth") int endMonth
    );

    /** Owner-scoped monthly revenue across a year span. */
    @Query("""
        SELECT p.year as year, p.month as month, COALESCE(SUM(p.amountPaid), 0) as revenue
        FROM Payment p
        WHERE p.status = 'PAID'
          AND p.agreement.property.owner.id = :ownerId
          AND ((p.year = :startYear AND p.month >= :startMonth) OR p.year > :startYear)
          AND ((p.year = :endYear AND p.month <= :endMonth) OR p.year < :endYear)
        GROUP BY p.year, p.month
        ORDER BY p.year, p.month
        """)
    List<Object[]> sumPaidByOwnerGroupedByMonth(
        @Param("ownerId") Long ownerId,
        @Param("startYear") int startYear, @Param("startMonth") int startMonth,
        @Param("endYear") int endYear, @Param("endMonth") int endMonth
    );

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = 'PAID'")
    long countByStatusPaid();

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = 'OVERDUE'")
    long countByStatusOverdue();

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = 'PENDING'")
    long countByStatusPending();

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = 'PAID' AND p.agreement.property.owner.id = :ownerId")
    long countByOwnerAndStatusPaid(@Param("ownerId") Long ownerId);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = 'OVERDUE' AND p.agreement.property.owner.id = :ownerId")
    long countByOwnerAndStatusOverdue(@Param("ownerId") Long ownerId);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = 'PENDING' AND p.agreement.property.owner.id = :ownerId")
    long countByOwnerAndStatusPending(@Param("ownerId") Long ownerId);

    /** Top revenue-generating properties, platform-wide. Use Pageable to limit (e.g. PageRequest.of(0, 5)). */
    @Query("""
        SELECT p.agreement.property.id as propertyId,
               p.agreement.property.name as propertyName,
               p.agreement.property.city as city,
               COALESCE(SUM(p.amountPaid), 0) as totalRevenue
        FROM Payment p
        WHERE p.status = 'PAID'
        GROUP BY p.agreement.property.id, p.agreement.property.name, p.agreement.property.city
        ORDER BY SUM(p.amountPaid) DESC
        """)
    List<Object[]> findTopPropertiesByRevenue(Pageable pageable);

    @Query("""
        SELECT p.agreement.property.id as propertyId,
               p.agreement.property.name as propertyName,
               p.agreement.property.city as city,
               COALESCE(SUM(p.amountPaid), 0) as totalRevenue
        FROM Payment p
        WHERE p.status = 'PAID' AND p.agreement.property.owner.id = :ownerId
        GROUP BY p.agreement.property.id, p.agreement.property.name, p.agreement.property.city
        ORDER BY SUM(p.amountPaid) DESC
        """)
    List<Object[]> findTopPropertiesByRevenueForOwner(@Param("ownerId") Long ownerId, Pageable pageable);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.tenant.id = :tenantId AND (p.status = 'PENDING' OR p.status = 'OVERDUE')")
    long countPendingByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.agreement.property.manager.id = :managerId AND (p.status = 'PENDING' OR p.status = 'OVERDUE')")
    long countPendingByManagerId(@Param("managerId") Long managerId);

    @Query("""
        SELECT COALESCE(SUM(p.amountDue - p.amountPaid), 0)
        FROM Payment p
        WHERE p.tenant.id = :tenantId AND (p.status = 'PENDING' OR p.status = 'OVERDUE')
        """)
    BigDecimal sumDueByTenantId(@Param("tenantId") Long tenantId);

    // ── Report-optimised queries (replaces findAll() in ReportService) ────────

    /**
     * Revenue report — platform-wide PAID payments filtered to a date range.
     * Admin uses this (no owner filter).
     */
    @Query("""
        SELECT p FROM Payment p
        JOIN FETCH p.agreement a
        JOIN FETCH a.property pr
        JOIN FETCH p.tenant t
        JOIN FETCH t.user tu
        WHERE p.status = 'PAID'
          AND p.paidDate >= :from
          AND p.paidDate <= :to
        ORDER BY p.paidDate DESC
        """)
    List<Payment> findPaidInRange(
        @Param("from") LocalDate from,
        @Param("to")   LocalDate to
    );

    /**
     * Revenue report — owner-scoped PAID payments filtered to a date range.
     */
    @Query("""
        SELECT p FROM Payment p
        JOIN FETCH p.agreement a
        JOIN FETCH a.property pr
        JOIN FETCH p.tenant t
        JOIN FETCH t.user tu
        WHERE p.status = 'PAID'
          AND p.paidDate >= :from
          AND p.paidDate <= :to
          AND pr.owner.id = :ownerId
        ORDER BY p.paidDate DESC
        """)
    List<Payment> findPaidInRangeForOwner(
        @Param("ownerId") Long ownerId,
        @Param("from")    LocalDate from,
        @Param("to")      LocalDate to
    );

    /**
     * Rent collection report — all payments for a given month/year.
     * Admin (no owner filter).
     */
    @Query("""
        SELECT p FROM Payment p
        JOIN FETCH p.agreement a
        JOIN FETCH a.property pr
        JOIN FETCH p.tenant t
        JOIN FETCH t.user tu
        WHERE p.month = :month AND p.year = :year
        ORDER BY pr.name, t.user.firstName
        """)
    List<Payment> findByMonthAndYear(
        @Param("month") int month,
        @Param("year")  int year
    );

    /**
     * Rent collection report — owner-scoped for a given month/year.
     */
    @Query("""
        SELECT p FROM Payment p
        JOIN FETCH p.agreement a
        JOIN FETCH a.property pr
        JOIN FETCH p.tenant t
        JOIN FETCH t.user tu
        WHERE p.month = :month AND p.year = :year
          AND pr.owner.id = :ownerId
        ORDER BY pr.name, t.user.firstName
        """)
    List<Payment> findByMonthAndYearForOwner(
        @Param("ownerId") Long ownerId,
        @Param("month")   int month,
        @Param("year")    int year
    );
}
