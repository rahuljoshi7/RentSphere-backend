package com.rentsphere.service.impl;

import com.rentsphere.dto.request.AgreementRequest;
import com.rentsphere.dto.response.AgreementResponse;
import com.rentsphere.dto.response.PagedResponse;
import com.rentsphere.entity.*;
import com.rentsphere.exception.BusinessException;
import com.rentsphere.exception.ResourceNotFoundException;
import com.rentsphere.exception.UnauthorizedException;
import com.rentsphere.repository.*;
import com.rentsphere.service.CloudinaryService;
import com.rentsphere.service.NotificationService;
import com.rentsphere.service.RentalAgreementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RentalAgreementServiceImpl implements RentalAgreementService {

    private final RentalAgreementRepository agreementRepository;
    private final PropertyRepository        propertyRepository;
    private final TenantRepository          tenantRepository;
    private final CloudinaryService         cloudinaryService;
    private final NotificationService       notificationService;

    @Override
    @Transactional
    public AgreementResponse create(AgreementRequest request, Long requesterId) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BusinessException("End date must be after start date.");
        }

        Property property = propertyRepository.findById(request.getPropertyId())
            .orElseThrow(() -> new ResourceNotFoundException("Property", request.getPropertyId()));

        if (agreementRepository.existsByPropertyIdAndStatus(
                property.getId(), RentalAgreement.AgreementStatus.ACTIVE)) {
            throw new BusinessException("Property already has an active agreement.");
        }

        Tenant tenant = tenantRepository.findById(request.getTenantId())
            .orElseThrow(() -> new ResourceNotFoundException("Tenant", request.getTenantId()));

        RentalAgreement agreement = RentalAgreement.builder()
            .property(property)
            .tenant(tenant)
            .startDate(request.getStartDate())
            .endDate(request.getEndDate())
            .monthlyRent(request.getMonthlyRent())
            .securityDeposit(request.getSecurityDeposit())
            .status(RentalAgreement.AgreementStatus.ACTIVE)
            .build();

        agreement = agreementRepository.save(agreement);

        // Mark property as occupied
        property.setAvailabilityStatus(Property.AvailabilityStatus.OCCUPIED);
        propertyRepository.save(property);

        log.info("Agreement created: id={} property={} tenant={}", agreement.getId(),
            property.getId(), tenant.getId());

        return toResponse(agreement);
    }

    @Override
    public AgreementResponse findById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    public PagedResponse<AgreementResponse> findByProperty(Long propertyId, int page, int size) {
        var result = agreementRepository.findByPropertyId(
            propertyId, PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return PagedResponse.of(result.map(this::toResponse));
    }

    @Override
    public PagedResponse<AgreementResponse> findByTenant(Long tenantId, int page, int size) {
        var result = agreementRepository.findByTenantId(
            tenantId, PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return PagedResponse.of(result.map(this::toResponse));
    }

    @Override
    public PagedResponse<AgreementResponse> findByOwner(Long ownerId, int page, int size) {
        var result = agreementRepository.findByOwnerId(
            ownerId, PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return PagedResponse.of(result.map(this::toResponse));
    }

    @Override
    @Transactional
    public AgreementResponse uploadDocument(Long id, MultipartFile file, Long requesterId) {
        RentalAgreement agreement = findOrThrow(id);
        assertOwnerOrManager(agreement, requesterId);

        // Delete old document if exists
        if (agreement.getCloudinaryDocId() != null) {
            cloudinaryService.deleteDocument(agreement.getCloudinaryDocId());
        }

        CloudinaryService.UploadResult result =
            cloudinaryService.uploadDocument(file, "agreements/" + id);

        agreement.setDocumentUrl(result.url());
        agreement.setCloudinaryDocId(result.publicId());
        return toResponse(agreementRepository.save(agreement));
    }

    @Override
    @Transactional
    public AgreementResponse terminate(Long id, Long requesterId) {
        RentalAgreement agreement = findOrThrow(id);
        assertOwnerOrManager(agreement, requesterId);

        if (!agreement.isActive()) {
            throw new BusinessException("Agreement is not currently active.");
        }

        agreement.setStatus(RentalAgreement.AgreementStatus.TERMINATED);
        freeProperty(agreement.getProperty());

        log.info("Agreement terminated: id={}", id);
        return toResponse(agreementRepository.save(agreement));
    }

    @Override
    @Transactional
    public AgreementResponse renew(Long id, AgreementRequest request, Long requesterId) {
        RentalAgreement old = findOrThrow(id);
        assertOwnerOrManager(old, requesterId);

        old.setStatus(RentalAgreement.AgreementStatus.RENEWED);
        agreementRepository.save(old);

        RentalAgreement renewed = RentalAgreement.builder()
            .property(old.getProperty())
            .tenant(old.getTenant())
            .startDate(request.getStartDate())
            .endDate(request.getEndDate())
            .monthlyRent(request.getMonthlyRent())
            .securityDeposit(request.getSecurityDeposit())
            .status(RentalAgreement.AgreementStatus.ACTIVE)
            .build();

        renewed = agreementRepository.save(renewed);
        log.info("Agreement renewed: oldId={} newId={}", id, renewed.getId());
        return toResponse(renewed);
    }

    @Override
    @Transactional
    public void checkAndExpireAgreements() {
        LocalDate today = LocalDate.now();
        var expired = agreementRepository.findExpired(today);
        expired.forEach(a -> {
            a.setStatus(RentalAgreement.AgreementStatus.EXPIRED);
            freeProperty(a.getProperty());
            notificationService.sendAgreementExpiryNotification(a);
        });
        if (!expired.isEmpty()) {
            agreementRepository.saveAll(expired);
            log.info("Expired {} agreements", expired.size());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void freeProperty(Property property) {
        property.setAvailabilityStatus(Property.AvailabilityStatus.AVAILABLE);
        propertyRepository.save(property);
    }

    private void assertOwnerOrManager(RentalAgreement agreement, Long requesterId) {
        Property p = agreement.getProperty();
        boolean ok = p.getOwner().getId().equals(requesterId)
            || (p.getManager() != null && p.getManager().getId().equals(requesterId));
        if (!ok) throw new UnauthorizedException("Access denied for this agreement.");
    }

    private RentalAgreement findOrThrow(Long id) {
        return agreementRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("RentalAgreement", id));
    }

    private AgreementResponse toResponse(RentalAgreement a) {
        return AgreementResponse.builder()
            .id(a.getId())
            .propertyId(a.getProperty().getId())
            .propertyName(a.getProperty().getName())
            .tenantId(a.getTenant().getId())
            .tenantName(a.getTenant().getFullName())
            .startDate(a.getStartDate())
            .endDate(a.getEndDate())
            .monthlyRent(a.getMonthlyRent())
            .securityDeposit(a.getSecurityDeposit())
            .status(a.getStatus())
            .documentUrl(a.getDocumentUrl())
            .createdAt(a.getCreatedAt())
            .updatedAt(a.getUpdatedAt())
            .build();
    }
}
