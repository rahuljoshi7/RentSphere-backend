package com.rentsphere.service;

import com.rentsphere.dto.response.*;
import com.rentsphere.entity.MaintenanceRequest.MaintenanceStatus;
import com.rentsphere.entity.MaintenanceRequest.Priority;
import com.rentsphere.entity.RentalAgreement;
import com.rentsphere.entity.RentalAgreement.AgreementStatus;
import com.rentsphere.entity.Tenant;
import com.rentsphere.exception.ResourceNotFoundException;
import com.rentsphere.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private static final int TREND_MONTHS = 6;
    private static final int EXPIRY_WINDOW_DAYS = 30;
    private static final int TOP_PROPERTIES_LIMIT = 5;

    private final PropertyRepository            propertyRepository;
    private final TenantRepository              tenantRepository;
    private final PaymentRepository             paymentRepository;
    private final RentalAgreementRepository     agreementRepository;
    private final MaintenanceRequestRepository  maintenanceRepository;

    // ── Existing summary dashboards ──────────────────────────────────────────

    public AdminDashboardResponse getAdminDashboard() {
        LocalDate now     = LocalDate.now();
        int month         = now.getMonthValue();
        int year          = now.getYear();

        long totalProperties    = propertyRepository.count();
        long occupiedProperties = propertyRepository.countOccupied();
        long vacantProperties   = propertyRepository.countAvailable();
        long totalTenants       = tenantRepository.countActive();
        long pendingPayments    = paymentRepository.countPending();
        long openMaintenance    = maintenanceRepository.countByStatus(MaintenanceStatus.OPEN);
        BigDecimal monthRevenue = paymentRepository.sumPaidByMonth(year, month);

        return AdminDashboardResponse.builder()
                .totalProperties(totalProperties)
                .occupiedProperties(occupiedProperties)
                .vacantProperties(vacantProperties)
                .totalTenants(totalTenants)
                .monthlyRevenue(monthRevenue != null ? monthRevenue : BigDecimal.ZERO)
                .pendingPayments(pendingPayments)
                .openMaintenanceRequests(openMaintenance)
                .activeAgreements(agreementRepository.countActive())
                .build();
    }

    public OwnerDashboardResponse getOwnerDashboard(Long ownerId) {
        LocalDate now = LocalDate.now();
        int month     = now.getMonthValue();
        int year      = now.getYear();

        long totalProperties    = propertyRepository.countByOwnerId(ownerId);
        long occupiedProperties = propertyRepository.countOccupiedByOwnerId(ownerId);
        long totalTenants       = tenantRepository.countActiveByOwnerId(ownerId);
        BigDecimal monthRevenue = paymentRepository.sumPaidByOwnerAndMonth(ownerId, year, month);
        BigDecimal yearRevenue  = paymentRepository.sumPaidByOwnerAndYear(ownerId, year);

        double occupancyRate = totalProperties > 0
                ? (double) occupiedProperties / totalProperties * 100 : 0;

        return OwnerDashboardResponse.builder()
                .totalProperties(totalProperties)
                .occupiedProperties(occupiedProperties)
                .vacantProperties(totalProperties - occupiedProperties)
                .totalTenants(totalTenants)
                .monthlyRevenue(monthRevenue != null ? monthRevenue : BigDecimal.ZERO)
                .yearlyRevenue(yearRevenue != null ? yearRevenue : BigDecimal.ZERO)
                .occupancyRate(Math.round(occupancyRate * 10.0) / 10.0)
                .build();
    }

    // ── Phase 7: Admin & Owner deep analytics ────────────────────────────────

    public AdminAnalyticsResponse getAdminAnalytics() {
        LocalDate today  = LocalDate.now();
        LocalDate cutoff = today.plusDays(EXPIRY_WINDOW_DAYS);

        List<RevenueTrendPoint> trend = buildTrend(
                paymentRepository.sumPaidGroupedByMonth(
                        today.minusMonths(TREND_MONTHS - 1).getYear(), today.minusMonths(TREND_MONTHS - 1).getMonthValue(),
                        today.getYear(), today.getMonthValue()
                )
        );

        PaymentStatusBreakdown paymentBreakdown = PaymentStatusBreakdown.builder()
                .paid(paymentRepository.countByStatusPaid())
                .pending(paymentRepository.countByStatusPending())
                .overdue(paymentRepository.countByStatusOverdue())
                .build();

        MaintenancePriorityBreakdown maintBreakdown = MaintenancePriorityBreakdown.builder()
                .low(maintenanceRepository.countByPriority(Priority.LOW))
                .medium(maintenanceRepository.countByPriority(Priority.MEDIUM))
                .high(maintenanceRepository.countByPriority(Priority.HIGH))
                .urgent(maintenanceRepository.countByPriority(Priority.URGENT))
                .build();

        List<TopPropertyResponse> topProperties = buildTopProperties(
                paymentRepository.findTopPropertiesByRevenue(PageRequest.of(0, TOP_PROPERTIES_LIMIT))
        );

        return AdminAnalyticsResponse.builder()
                .revenueTrend(trend)
                .paymentStatusBreakdown(paymentBreakdown)
                .maintenancePriorityBreakdown(maintBreakdown)
                .topProperties(topProperties)
                .agreementsExpiringSoon(agreementRepository.countExpiringSoon(today, cutoff))
                .build();
    }

    public OwnerAnalyticsResponse getOwnerAnalytics(Long ownerId) {
        LocalDate today  = LocalDate.now();
        LocalDate cutoff = today.plusDays(EXPIRY_WINDOW_DAYS);

        List<RevenueTrendPoint> trend = buildTrend(
                paymentRepository.sumPaidByOwnerGroupedByMonth(
                        ownerId,
                        today.minusMonths(TREND_MONTHS - 1).getYear(), today.minusMonths(TREND_MONTHS - 1).getMonthValue(),
                        today.getYear(), today.getMonthValue()
                )
        );

        PaymentStatusBreakdown paymentBreakdown = PaymentStatusBreakdown.builder()
                .paid(paymentRepository.countByOwnerAndStatusPaid(ownerId))
                .pending(paymentRepository.countByOwnerAndStatusPending(ownerId))
                .overdue(paymentRepository.countByOwnerAndStatusOverdue(ownerId))
                .build();

        List<TopPropertyResponse> topProperties = buildTopProperties(
                paymentRepository.findTopPropertiesByRevenueForOwner(ownerId, PageRequest.of(0, TOP_PROPERTIES_LIMIT))
        );

        return OwnerAnalyticsResponse.builder()
                .revenueTrend(trend)
                .paymentStatusBreakdown(paymentBreakdown)
                .topProperties(topProperties)
                .agreementsExpiringSoon(agreementRepository.countExpiringSoonByOwnerId(ownerId, today, cutoff))
                .build();
    }

    // ── Phase 7: Manager dashboard ───────────────────────────────────────────

    public ManagerDashboardResponse getManagerDashboard(Long managerId) {
        long managedProperties  = propertyRepository.countByManagerId(managerId);
        long occupiedProperties = propertyRepository.countOccupiedByManagerId(managerId);
        long activeTenants      = tenantRepository.countActiveByManagerId(managerId);
        long activeAgreements   = agreementRepository.countActiveByManagerId(managerId);
        long openMaintenance    = maintenanceRepository.countOpenByManagerId(managerId);
        long pendingPayments    = paymentRepository.countPendingByManagerId(managerId);

        double occupancyRate = managedProperties > 0
                ? (double) occupiedProperties / managedProperties * 100 : 0;

        return ManagerDashboardResponse.builder()
                .managedProperties(managedProperties)
                .occupiedProperties(occupiedProperties)
                .vacantProperties(managedProperties - occupiedProperties)
                .activeTenants(activeTenants)
                .activeAgreements(activeAgreements)
                .openMaintenanceRequests(openMaintenance)
                .pendingPayments(pendingPayments)
                .occupancyRate(Math.round(occupancyRate * 10.0) / 10.0)
                .build();
    }

    // ── Phase 7: Tenant dashboard ────────────────────────────────────────────

    public TenantDashboardResponse getTenantDashboard(Long userId) {
        Tenant tenant = tenantRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant profile not found for current user."));

        RentalAgreement active = agreementRepository
                .findByTenantIdAndStatus(tenant.getId(), AgreementStatus.ACTIVE)
                .orElse(null);

        LocalDate today = LocalDate.now();
        boolean expiringSoon = active != null
                && !active.getEndDate().isBefore(today)
                && !active.getEndDate().isAfter(today.plusDays(EXPIRY_WINDOW_DAYS));

        long pendingPayments = paymentRepository.countPendingByTenantId(tenant.getId());
        BigDecimal totalDue  = paymentRepository.sumDueByTenantId(tenant.getId());

        long totalMaintenance = maintenanceRepository.countByTenantId(tenant.getId());
        long openMaintenance  = maintenanceRepository.countByTenantIdAndStatusNot(tenant.getId(), MaintenanceStatus.COMPLETED);

        return TenantDashboardResponse.builder()
                .activePropertyName(active != null ? active.getProperty().getName() : null)
                .agreementEndDate(active != null ? active.getEndDate() : null)
                .monthlyRent(active != null ? active.getMonthlyRent() : BigDecimal.ZERO)
                .pendingPayments(pendingPayments)
                .totalDue(totalDue != null ? totalDue : BigDecimal.ZERO)
                .openMaintenanceRequests(openMaintenance)
                .totalMaintenanceRequests(totalMaintenance)
                .agreementExpiringSoon(expiringSoon)
                .build();
    }

    // ── Phase 7: Maintenance staff dashboard ─────────────────────────────────

    public StaffDashboardResponse getStaffDashboard(Long staffId) {
        long totalAssigned   = maintenanceRepository.countAssignedToStaff(staffId);
        long openCount       = maintenanceRepository.countAssignedToStaffByStatus(staffId, MaintenanceStatus.OPEN)
                + maintenanceRepository.countAssignedToStaffByStatus(staffId, MaintenanceStatus.ASSIGNED);
        long inProgressCount = maintenanceRepository.countAssignedToStaffByStatus(staffId, MaintenanceStatus.IN_PROGRESS);
        long completedCount  = maintenanceRepository.countAssignedToStaffByStatus(staffId, MaintenanceStatus.COMPLETED);

        MaintenancePriorityBreakdown priorityBreakdown = MaintenancePriorityBreakdown.builder()
                .low(maintenanceRepository.countAssignedToStaffByPriority(staffId, Priority.LOW))
                .medium(maintenanceRepository.countAssignedToStaffByPriority(staffId, Priority.MEDIUM))
                .high(maintenanceRepository.countAssignedToStaffByPriority(staffId, Priority.HIGH))
                .urgent(maintenanceRepository.countAssignedToStaffByPriority(staffId, Priority.URGENT))
                .build();

        return StaffDashboardResponse.builder()
                .totalAssigned(totalAssigned)
                .openCount(openCount)
                .inProgressCount(inProgressCount)
                .completedCount(completedCount)
                .priorityBreakdown(priorityBreakdown)
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<RevenueTrendPoint> buildTrend(List<Object[]> rows) {
        List<RevenueTrendPoint> points = new ArrayList<>();
        for (Object[] row : rows) {
            int year       = (int) row[0];
            int month      = (int) row[1];
            BigDecimal rev = (BigDecimal) row[2];
            points.add(RevenueTrendPoint.builder()
                    .year(year)
                    .month(month)
                    .label(monthLabel(month) + " " + year)
                    .revenue(rev != null ? rev : BigDecimal.ZERO)
                    .build());
        }
        return points;
    }

    private List<TopPropertyResponse> buildTopProperties(List<Object[]> rows) {
        List<TopPropertyResponse> results = new ArrayList<>();
        for (Object[] row : rows) {
            results.add(TopPropertyResponse.builder()
                    .propertyId((Long) row[0])
                    .propertyName((String) row[1])
                    .city((String) row[2])
                    .totalRevenue((BigDecimal) row[3])
                    .build());
        }
        return results;
    }

    private String monthLabel(int month) {
        return java.time.Month.of(month).getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
    }
}
