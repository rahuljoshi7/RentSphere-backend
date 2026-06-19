package com.rentsphere.service;

import com.rentsphere.dto.request.AgreementRequest;
import com.rentsphere.dto.response.AgreementResponse;
import com.rentsphere.dto.response.PagedResponse;
import org.springframework.web.multipart.MultipartFile;

public interface RentalAgreementService {
    AgreementResponse create(AgreementRequest request, Long requesterId);
    AgreementResponse findById(Long id);
    PagedResponse<AgreementResponse> findByProperty(Long propertyId, int page, int size);
    PagedResponse<AgreementResponse> findByTenant(Long tenantId, int page, int size);
    PagedResponse<AgreementResponse> findByOwner(Long ownerId, int page, int size);
    AgreementResponse uploadDocument(Long id, MultipartFile file, Long requesterId);
    AgreementResponse terminate(Long id, Long requesterId);
    AgreementResponse renew(Long id, AgreementRequest request, Long requesterId);
    void checkAndExpireAgreements();   // called by scheduler
}
