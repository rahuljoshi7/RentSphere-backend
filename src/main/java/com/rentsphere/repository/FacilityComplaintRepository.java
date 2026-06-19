package com.rentsphere.repository;

import com.rentsphere.entity.FacilityComplaint;
import com.rentsphere.entity.FacilityComplaint.ComplaintStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FacilityComplaintRepository extends JpaRepository<FacilityComplaint, Long> {

    Page<FacilityComplaint> findByFacilityId(Long facilityId, Pageable pageable);

    Page<FacilityComplaint> findByTenantId(Long tenantId, Pageable pageable);

    Page<FacilityComplaint> findByFacilityPropertyIdAndStatus(
        Long propertyId, ComplaintStatus status, Pageable pageable
    );
}
