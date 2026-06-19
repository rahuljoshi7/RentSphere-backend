package com.rentsphere.service;

import com.rentsphere.dto.request.TenantProfileRequest;
import com.rentsphere.dto.response.PagedResponse;
import com.rentsphere.dto.response.TenantResponse;

public interface TenantService {
    TenantResponse findById(Long tenantId);
    TenantResponse findByUserId(Long userId);
    TenantResponse updateProfile(Long tenantId, TenantProfileRequest request, Long requesterId);
    PagedResponse<TenantResponse> findAll(int page, int size);
    PagedResponse<TenantResponse> search(String query, int page, int size);
    PagedResponse<TenantResponse> findActiveByOwner(Long ownerId, int page, int size);
    void deactivate(Long tenantId, Long requesterId);
}
