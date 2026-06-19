package com.rentsphere.service.impl;

import com.rentsphere.dto.request.PaymentRequest;
import com.rentsphere.dto.response.PagedResponse;
import com.rentsphere.dto.response.PaymentResponse;
import com.rentsphere.entity.Payment;
import com.rentsphere.entity.RentalAgreement;
import com.rentsphere.exception.BusinessException;
import com.rentsphere.exception.ResourceNotFoundException;
import com.rentsphere.exception.UnauthorizedException;
import com.rentsphere.repository.PaymentRepository;
import com.rentsphere.repository.RentalAgreementRepository;
import com.rentsphere.service.NotificationService;
import com.rentsphere.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository         paymentRepository;
    private final RentalAgreementRepository agreementRepository;
    private final NotificationService       notificationService;

    // ── Generate single month payment ─────────────────────────────────────────

    @Override
    @Transactional
    public PaymentResponse generateMonthlyPayment(Long agreementId, int month, int year, Long requesterId) {
        RentalAgreement agreement = findAgreementOrThrow(agreementId);
        assertOwnerOrManager(agreement, requesterId);

        if (!agreement.isActive()) {
            throw new BusinessException("Cannot generate payment for an inactive agreement.");
        }

        if (paymentRepository.findByAgreementIdAndMonthAndYear(agreementId, month, year).isPresent()) {
            throw new BusinessException("Payment for " + month + "/" + year + " already exists.");
        }

        Payment payment = Payment.builder()
            .agreement(agreement)
            .tenant(agreement.getTenant())
            .month(month)
            .year(year)
            .amountDue(agreement.getMonthlyRent())
            .dueDate(LocalDate.of(year, month, 5))   // Due on 5th of each month
            .status(Payment.PaymentStatus.PENDING)
            .build();

        payment = paymentRepository.save(payment);
        log.info("Payment generated: agreementId={} month={}/{}", agreementId, month, year);
        return toResponse(payment);
    }

    // ── Record a payment ──────────────────────────────────────────────────────

    @Override
    @Transactional
    public PaymentResponse recordPayment(Long paymentId, PaymentRequest request, Long requesterId) {
        Payment payment = findOrThrow(paymentId);
        assertOwnerOrManager(payment.getAgreement(), requesterId);

        if (payment.getStatus() == Payment.PaymentStatus.PAID) {
            throw new BusinessException("Payment has already been fully recorded.");
        }

        payment.markAsPaid(request.getAmountPaid());
        if (request.getNotes() != null) payment.setNotes(request.getNotes());

        payment = paymentRepository.save(payment);
        log.info("Payment recorded: id={} amount={}", paymentId, request.getAmountPaid());
        return toResponse(payment);
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @Override
    public PaymentResponse findById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    public PagedResponse<PaymentResponse> findByTenant(Long tenantId, int page, int size) {
        var result = paymentRepository.findByTenantId(
            tenantId, PageRequest.of(page, size, Sort.by("year", "month").descending()));
        return PagedResponse.of(result.map(this::toResponse));
    }

    @Override
    public PagedResponse<PaymentResponse> findByAgreement(Long agreementId, int page, int size) {
        var result = paymentRepository.findByAgreementId(
            agreementId, PageRequest.of(page, size, Sort.by("year", "month").descending()));
        return PagedResponse.of(result.map(this::toResponse));
    }

    @Override
    public PagedResponse<PaymentResponse> findByOwner(Long ownerId, int page, int size) {
        var result = paymentRepository.findByOwnerId(
            ownerId, PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return PagedResponse.of(result.map(this::toResponse));
    }

    @Override
    public PagedResponse<PaymentResponse> findByStatus(Payment.PaymentStatus status, int page, int size) {
        var result = paymentRepository.findByStatus(
            status, PageRequest.of(page, size, Sort.by("dueDate").ascending()));
        return PagedResponse.of(result.map(this::toResponse));
    }

    // ── Scheduler methods ─────────────────────────────────────────────────────

    @Override
    @Transactional
    public void generateMonthlyRentForAllActive(int month, int year) {
        List<RentalAgreement> active = agreementRepository.findAll().stream()
            .filter(RentalAgreement::isActive)
            .toList();

        int generated = 0;
        for (RentalAgreement agreement : active) {
            boolean alreadyExists = paymentRepository
                .findByAgreementIdAndMonthAndYear(agreement.getId(), month, year)
                .isPresent();

            if (!alreadyExists) {
                Payment payment = Payment.builder()
                    .agreement(agreement)
                    .tenant(agreement.getTenant())
                    .month(month)
                    .year(year)
                    .amountDue(agreement.getMonthlyRent())
                    .dueDate(LocalDate.of(year, month, 5))
                    .status(Payment.PaymentStatus.PENDING)
                    .build();

                paymentRepository.save(payment);

                // Send rent due notification
                notificationService.sendRentDueNotification(payment);
                generated++;
            }
        }
        log.info("Bulk payment generation: {} payments created for {}/{}", generated, month, year);
    }

    @Override
    @Transactional
    public void markOverduePayments() {
        List<Payment> overdue = paymentRepository.findPendingOverdue(LocalDate.now());
        overdue.forEach(Payment::checkAndMarkOverdue);
        if (!overdue.isEmpty()) {
            paymentRepository.saveAll(overdue);
            log.info("Marked {} payments as overdue", overdue.size());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Payment findOrThrow(Long id) {
        return paymentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Payment", id));
    }

    private RentalAgreement findAgreementOrThrow(Long id) {
        return agreementRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("RentalAgreement", id));
    }

    private void assertOwnerOrManager(RentalAgreement agreement, Long requesterId) {
        var property = agreement.getProperty();
        boolean ok = property.getOwner().getId().equals(requesterId)
            || (property.getManager() != null && property.getManager().getId().equals(requesterId));
        if (!ok) throw new UnauthorizedException("Access denied for this payment.");
    }

    private PaymentResponse toResponse(Payment p) {
        return PaymentResponse.builder()
            .id(p.getId())
            .agreementId(p.getAgreement().getId())
            .tenantId(p.getTenant().getId())
            .tenantName(p.getTenant().getFullName())
            .propertyName(p.getAgreement().getProperty().getName())
            .month(p.getMonth())
            .year(p.getYear())
            .amountDue(p.getAmountDue())
            .amountPaid(p.getAmountPaid())
            .balance(p.getBalance())
            .status(p.getStatus())
            .dueDate(p.getDueDate())
            .paidDate(p.getPaidDate())
            .notes(p.getNotes())
            .createdAt(p.getCreatedAt())
            .build();
    }
}
