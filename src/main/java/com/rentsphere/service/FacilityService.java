package com.rentsphere.service;

import com.rentsphere.dto.request.FacilityComplaintRequest;
import com.rentsphere.dto.request.FacilityRequest;
import com.rentsphere.dto.response.FacilityComplaintResponse;
import com.rentsphere.dto.response.FacilityResponse;
import com.rentsphere.dto.response.PagedResponse;
import com.rentsphere.entity.FacilityComplaint;

import java.util.List;

public interface FacilityService {
    FacilityResponse addFacility(Long propertyId, FacilityRequest request, Long requesterId);
    FacilityResponse updateFacility(Long facilityId, FacilityRequest request, Long requesterId);
    List<FacilityResponse> findByProperty(Long propertyId);
    FacilityComplaintResponse raiseComplaint(FacilityComplaintRequest request, Long tenantUserId);
    FacilityComplaintResponse resolveComplaint(Long complaintId, Long requesterId);
    PagedResponse<FacilityComplaintResponse> findComplaintsByProperty(
        Long propertyId, FacilityComplaint.ComplaintStatus status, int page, int size);
    PagedResponse<FacilityComplaintResponse> findComplaintsByTenant(Long tenantId, int page, int size);
}
