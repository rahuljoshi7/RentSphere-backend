package com.rentsphere.service;

import com.rentsphere.dto.request.MaintenanceRequest;
import com.rentsphere.dto.request.MaintenanceStatusRequest;
import com.rentsphere.dto.response.MaintenanceResponse;
import com.rentsphere.dto.response.PagedResponse;
import com.rentsphere.entity.MaintenanceRequest.MaintenanceStatus;
import org.springframework.web.multipart.MultipartFile;

public interface MaintenanceService {
    MaintenanceResponse create(MaintenanceRequest request, Long tenantUserId);
    MaintenanceResponse findById(Long id);
    PagedResponse<MaintenanceResponse> findByTenant(Long tenantId, int page, int size);
    PagedResponse<MaintenanceResponse> findByProperty(Long propertyId, int page, int size);
    PagedResponse<MaintenanceResponse> findByStatus(MaintenanceStatus status, int page, int size);
    PagedResponse<MaintenanceResponse> findAssignedToStaff(Long staffId, int page, int size);
    PagedResponse<MaintenanceResponse> findByOwner(Long ownerId, int page, int size);
    MaintenanceResponse assignToStaff(Long requestId, Long staffId, Long requesterId);
    MaintenanceResponse updateStatus(Long requestId, MaintenanceStatusRequest req, Long requesterId);
    MaintenanceResponse uploadImage(Long requestId, MultipartFile file, Long tenantUserId);
    void close(Long requestId, Long staffId);
}
