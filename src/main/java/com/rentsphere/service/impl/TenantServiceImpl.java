package com.rentsphere.service.impl;

import com.rentsphere.dto.request.TenantProfileRequest;
import com.rentsphere.dto.response.PagedResponse;
import com.rentsphere.dto.response.TenantResponse;
import com.rentsphere.entity.Tenant;
import com.rentsphere.entity.User;
import com.rentsphere.exception.ResourceNotFoundException;
import com.rentsphere.exception.UnauthorizedException;
import com.rentsphere.repository.TenantRepository;
import com.rentsphere.repository.UserRepository;
import com.rentsphere.service.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TenantServiceImpl implements TenantService {

    private final TenantRepository tenantRepository;
    private final UserRepository   userRepository;

    @Override
    public TenantResponse findById(Long tenantId) {
        return toResponse(findOrThrow(tenantId));
    }

    @Override
    public TenantResponse findByUserId(Long userId) {
        return toResponse(tenantRepository.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Tenant profile not found for user: " + userId)));
    }

    @Override
    @Transactional
    public TenantResponse updateProfile(Long tenantId, TenantProfileRequest request, Long requesterId) {
        Tenant tenant = findOrThrow(tenantId);

        if (!tenant.getUser().getId().equals(requesterId)) {
            throw new UnauthorizedException("You can only update your own profile.");
        }

        tenant.setAadhaarRef(request.getAadhaarRef());
        tenant.setPanRef(request.getPanRef());
        tenant.setCurrentAddress(request.getCurrentAddress());
        tenant.setEmergencyContactName(request.getEmergencyContactName());
        tenant.setEmergencyContactPhone(request.getEmergencyContactPhone());

        return toResponse(tenantRepository.save(tenant));
    }

    @Override
    public PagedResponse<TenantResponse> findAll(int page, int size) {
        var result = tenantRepository.findAll(PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return PagedResponse.of(result.map(this::toResponse));
    }

    @Override
    public PagedResponse<TenantResponse> search(String query, int page, int size) {
        var result = tenantRepository.search(query, PageRequest.of(page, size));
        return PagedResponse.of(result.map(this::toResponse));
    }

    @Override
    public PagedResponse<TenantResponse> findActiveByOwner(Long ownerId, int page, int size) {
        var result = tenantRepository.findActiveByOwnerId(ownerId, PageRequest.of(page, size));
        return PagedResponse.of(result.map(this::toResponse));
    }

    @Override
    @Transactional
    public void deactivate(Long tenantId, Long requesterId) {
        Tenant tenant = findOrThrow(tenantId);
        User user = tenant.getUser();
        user.setIsActive(false);
        userRepository.save(user);
    }

    private Tenant findOrThrow(Long id) {
        return tenantRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Tenant", id));
    }

    private TenantResponse toResponse(Tenant t) {
        User u = t.getUser();
        return TenantResponse.builder()
            .id(t.getId())
            .userId(u.getId())
            .firstName(u.getFirstName())
            .lastName(u.getLastName())
            .email(u.getEmail())
            .phone(u.getPhone())
            .aadhaarRef(t.getAadhaarRef())
            .panRef(t.getPanRef())
            .currentAddress(t.getCurrentAddress())
            .emergencyContactName(t.getEmergencyContactName())
            .emergencyContactPhone(t.getEmergencyContactPhone())
            .createdAt(t.getCreatedAt())
            .build();
    }
}
