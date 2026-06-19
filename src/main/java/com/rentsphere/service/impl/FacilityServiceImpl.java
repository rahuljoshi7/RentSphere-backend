package com.rentsphere.service.impl;

import com.rentsphere.dto.request.FacilityComplaintRequest;
import com.rentsphere.dto.request.FacilityRequest;
import com.rentsphere.dto.response.FacilityComplaintResponse;
import com.rentsphere.dto.response.FacilityResponse;
import com.rentsphere.dto.response.PagedResponse;
import com.rentsphere.entity.*;
import com.rentsphere.exception.BusinessException;
import com.rentsphere.exception.ResourceNotFoundException;
import com.rentsphere.exception.UnauthorizedException;
import com.rentsphere.repository.*;
import com.rentsphere.service.FacilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FacilityServiceImpl implements FacilityService {

    private final FacilityRepository          facilityRepository;
    private final FacilityComplaintRepository complaintRepository;
    private final PropertyRepository          propertyRepository;
    private final TenantRepository            tenantRepository;

    @Override
    @Transactional
    public FacilityResponse addFacility(Long propertyId, FacilityRequest request, Long requesterId) {
        Property property = findPropertyOrThrow(propertyId);
        assertOwnerOrManager(property, requesterId);

        if (facilityRepository.existsByPropertyIdAndFacilityType(propertyId, request.getFacilityType())) {
            throw new BusinessException(request.getFacilityType() + " already exists for this property.");
        }

        Facility facility = Facility.builder()
            .property(property)
            .facilityType(request.getFacilityType())
            .status(request.getStatus())
            .description(request.getDescription())
            .build();

        return toResponse(facilityRepository.save(facility));
    }

    @Override
    @Transactional
    public FacilityResponse updateFacility(Long facilityId, FacilityRequest request, Long requesterId) {
        Facility facility = findFacilityOrThrow(facilityId);
        assertOwnerOrManager(facility.getProperty(), requesterId);

        facility.setStatus(request.getStatus());
        facility.setDescription(request.getDescription());
        return toResponse(facilityRepository.save(facility));
    }

    @Override
    public List<FacilityResponse> findByProperty(Long propertyId) {
        return facilityRepository.findByPropertyId(propertyId).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public FacilityComplaintResponse raiseComplaint(FacilityComplaintRequest request, Long tenantUserId) {
        Tenant tenant = tenantRepository.findByUserId(tenantUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Tenant profile not found."));

        Facility facility = findFacilityOrThrow(request.getFacilityId());

        FacilityComplaint complaint = FacilityComplaint.builder()
            .facility(facility)
            .tenant(tenant)
            .complaintText(request.getComplaintText())
            .build();

        return toComplaintResponse(complaintRepository.save(complaint));
    }

    @Override
    @Transactional
    public FacilityComplaintResponse resolveComplaint(Long complaintId, Long requesterId) {
        FacilityComplaint complaint = complaintRepository.findById(complaintId)
            .orElseThrow(() -> new ResourceNotFoundException("FacilityComplaint", complaintId));

        assertOwnerOrManager(complaint.getFacility().getProperty(), requesterId);
        complaint.resolve();
        return toComplaintResponse(complaintRepository.save(complaint));
    }

    @Override
    public PagedResponse<FacilityComplaintResponse> findComplaintsByProperty(
            Long propertyId, FacilityComplaint.ComplaintStatus status, int page, int size) {
        var result = complaintRepository.findByFacilityPropertyIdAndStatus(
            propertyId, status, PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return PagedResponse.of(result.map(this::toComplaintResponse));
    }

    @Override
    public PagedResponse<FacilityComplaintResponse> findComplaintsByTenant(Long tenantId, int page, int size) {
        var result = complaintRepository.findByTenantId(
            tenantId, PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return PagedResponse.of(result.map(this::toComplaintResponse));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Property findPropertyOrThrow(Long id) {
        return propertyRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Property", id));
    }

    private Facility findFacilityOrThrow(Long id) {
        return facilityRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Facility", id));
    }

    private void assertOwnerOrManager(Property property, Long requesterId) {
        boolean ok = property.getOwner().getId().equals(requesterId)
            || (property.getManager() != null && property.getManager().getId().equals(requesterId));
        if (!ok) throw new UnauthorizedException("Access denied.");
    }

    private FacilityResponse toResponse(Facility f) {
        return FacilityResponse.builder()
            .id(f.getId())
            .propertyId(f.getProperty().getId())
            .facilityType(f.getFacilityType())
            .status(f.getStatus())
            .description(f.getDescription())
            .createdAt(f.getCreatedAt())
            .build();
    }

    private FacilityComplaintResponse toComplaintResponse(FacilityComplaint c) {
        return FacilityComplaintResponse.builder()
            .id(c.getId())
            .facilityId(c.getFacility().getId())
            .facilityType(c.getFacility().getFacilityType())
            .tenantId(c.getTenant().getId())
            .tenantName(c.getTenant().getFullName())
            .complaintText(c.getComplaintText())
            .status(c.getStatus())
            .createdAt(c.getCreatedAt())
            .resolvedAt(c.getResolvedAt())
            .build();
    }
}
