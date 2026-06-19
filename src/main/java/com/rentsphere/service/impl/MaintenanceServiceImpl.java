package com.rentsphere.service.impl;

import com.rentsphere.dto.request.MaintenanceRequest;
import com.rentsphere.dto.request.MaintenanceStatusRequest;
import com.rentsphere.dto.response.MaintenanceResponse;
import com.rentsphere.dto.response.PagedResponse;
import com.rentsphere.entity.*;
import com.rentsphere.entity.MaintenanceRequest.MaintenanceStatus;
import com.rentsphere.exception.BusinessException;
import com.rentsphere.exception.ResourceNotFoundException;
import com.rentsphere.exception.UnauthorizedException;
import com.rentsphere.repository.*;
import com.rentsphere.service.CloudinaryService;
import com.rentsphere.service.MaintenanceService;
import com.rentsphere.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MaintenanceServiceImpl implements MaintenanceService {

    private final MaintenanceRequestRepository    requestRepository;
    private final MaintenanceAssignmentRepository assignmentRepository;
    private final TenantRepository                tenantRepository;
    private final PropertyRepository              propertyRepository;
    private final UserRepository                  userRepository;
    private final CloudinaryService               cloudinaryService;
    private final NotificationService             notificationService;

    @Override
    @Transactional
    public MaintenanceResponse create(MaintenanceRequest dto, Long tenantUserId) {
        Tenant tenant = tenantRepository.findByUserId(tenantUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Tenant profile not found for user: " + tenantUserId));

        Property property = propertyRepository.findById(dto.getPropertyId())
            .orElseThrow(() -> new ResourceNotFoundException("Property", dto.getPropertyId()));

        com.rentsphere.entity.MaintenanceRequest request =
            com.rentsphere.entity.MaintenanceRequest.builder()
                .property(property)
                .tenant(tenant)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .priority(dto.getPriority())
                .status(MaintenanceStatus.OPEN)
                .build();

        request = requestRepository.save(request);
        log.info("Maintenance request created: id={} by tenantId={}", request.getId(), tenant.getId());

        // Notify property owner
        notificationService.sendMaintenanceUpdateNotification(request);
        return toResponse(request);
    }

    @Override
    public MaintenanceResponse findById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    public PagedResponse<MaintenanceResponse> findByTenant(Long tenantId, int page, int size) {
        var result = requestRepository.findByTenantId(tenantId, pageOf(page, size));
        return PagedResponse.of(result.map(this::toResponse));
    }

    @Override
    public PagedResponse<MaintenanceResponse> findByProperty(Long propertyId, int page, int size) {
        var result = requestRepository.findByPropertyId(propertyId, pageOf(page, size));
        return PagedResponse.of(result.map(this::toResponse));
    }

    @Override
    public PagedResponse<MaintenanceResponse> findByStatus(MaintenanceStatus status, int page, int size) {
        var result = requestRepository.findByStatus(status, pageOf(page, size));
        return PagedResponse.of(result.map(this::toResponse));
    }

    @Override
    public PagedResponse<MaintenanceResponse> findAssignedToStaff(Long staffId, int page, int size) {
        var result = requestRepository.findAssignedToStaff(staffId, pageOf(page, size));
        return PagedResponse.of(result.map(this::toResponse));
    }

    @Override
    public PagedResponse<MaintenanceResponse> findByOwner(Long ownerId, int page, int size) {
        var result = requestRepository.findByOwnerId(ownerId, pageOf(page, size));
        return PagedResponse.of(result.map(this::toResponse));
    }

    @Override
    @Transactional
    public MaintenanceResponse assignToStaff(Long requestId, Long staffId, Long requesterId) {
        com.rentsphere.entity.MaintenanceRequest request = findOrThrow(requestId);
        assertOwnerOrManager(request, requesterId);

        if (assignmentRepository.existsByRequestIdAndStaffId(requestId, staffId)) {
            throw new BusinessException("Staff member is already assigned to this request.");
        }

        User staff = userRepository.findById(staffId)
            .orElseThrow(() -> new ResourceNotFoundException("User", staffId));

        MaintenanceAssignment assignment = MaintenanceAssignment.builder()
            .request(request)
            .staff(staff)
            .build();

        assignmentRepository.save(assignment);
        request.setStatus(MaintenanceStatus.ASSIGNED);
        requestRepository.save(request);

        notificationService.sendMaintenanceUpdateNotification(request);
        log.info("Request {} assigned to staff {}", requestId, staffId);
        return toResponse(request);
    }

    @Override
    @Transactional
    public MaintenanceResponse updateStatus(Long requestId, MaintenanceStatusRequest dto, Long requesterId) {
        com.rentsphere.entity.MaintenanceRequest request = findOrThrow(requestId);

        // Staff can update their own assigned requests; owners/managers can update any
        boolean isStaff = assignmentRepository.findByRequestId(requestId).stream()
            .anyMatch(a -> a.getStaff().getId().equals(requesterId));
        boolean isOwnerOrManager = request.getProperty().getOwner().getId().equals(requesterId)
            || (request.getProperty().getManager() != null
                && request.getProperty().getManager().getId().equals(requesterId));

        if (!isStaff && !isOwnerOrManager) {
            throw new UnauthorizedException("You are not authorized to update this request.");
        }

        if (request.isCompleted()) {
            throw new BusinessException("Cannot update a completed request.");
        }

        request.setStatus(dto.getStatus());

        if (dto.getStatus() == MaintenanceStatus.COMPLETED) {
            request.close();
            // Mark assignment as completed
            assignmentRepository.findByRequestId(requestId).stream()
                .filter(a -> a.getStaff().getId().equals(requesterId))
                .findFirst()
                .ifPresent(a -> {
                    a.markCompleted();
                    assignmentRepository.save(a);
                });
        }

        requestRepository.save(request);
        notificationService.sendMaintenanceUpdateNotification(request);
        return toResponse(request);
    }

    @Override
    @Transactional
    public MaintenanceResponse uploadImage(Long requestId, MultipartFile file, Long tenantUserId) {
        com.rentsphere.entity.MaintenanceRequest request = findOrThrow(requestId);

        Tenant tenant = tenantRepository.findByUserId(tenantUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Tenant profile not found."));

        if (!request.getTenant().getId().equals(tenant.getId())) {
            throw new UnauthorizedException("You can only upload images for your own requests.");
        }

        CloudinaryService.UploadResult result =
            cloudinaryService.uploadImage(file, "maintenance/" + requestId);

        request.setImageUrl(result.url());
        return toResponse(requestRepository.save(request));
    }

    @Override
    @Transactional
    public void close(Long requestId, Long staffId) {
        com.rentsphere.entity.MaintenanceRequest request = findOrThrow(requestId);
        request.close();
        requestRepository.save(request);
        log.info("Request {} closed by staff {}", requestId, staffId);
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private MaintenanceResponse toResponse(com.rentsphere.entity.MaintenanceRequest r) {
        List<MaintenanceResponse.AssignmentInfo> assignments =
            assignmentRepository.findByRequestId(r.getId()).stream()
                .map(a -> MaintenanceResponse.AssignmentInfo.builder()
                    .staffId(a.getStaff().getId())
                    .staffName(a.getStaff().getFullName())
                    .notes(a.getNotes())
                    .assignedAt(a.getAssignedAt())
                    .completedAt(a.getCompletedAt())
                    .build())
                .collect(Collectors.toList());

        return MaintenanceResponse.builder()
            .id(r.getId())
            .propertyId(r.getProperty().getId())
            .propertyName(r.getProperty().getName())
            .tenantId(r.getTenant().getId())
            .tenantName(r.getTenant().getFullName())
            .title(r.getTitle())
            .description(r.getDescription())
            .status(r.getStatus())
            .priority(r.getPriority())
            .imageUrl(r.getImageUrl())
            .assignments(assignments)
            .createdAt(r.getCreatedAt())
            .updatedAt(r.getUpdatedAt())
            .closedAt(r.getClosedAt())
            .build();
    }

    private com.rentsphere.entity.MaintenanceRequest findOrThrow(Long id) {
        return requestRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("MaintenanceRequest", id));
    }

    private void assertOwnerOrManager(com.rentsphere.entity.MaintenanceRequest req, Long requesterId) {
        Property p = req.getProperty();
        boolean ok = p.getOwner().getId().equals(requesterId)
            || (p.getManager() != null && p.getManager().getId().equals(requesterId));
        if (!ok) throw new UnauthorizedException("Access denied for this maintenance request.");
    }

    private PageRequest pageOf(int page, int size) {
        return PageRequest.of(page, size, Sort.by("createdAt").descending());
    }
}
