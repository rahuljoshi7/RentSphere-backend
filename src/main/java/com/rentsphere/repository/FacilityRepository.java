package com.rentsphere.repository;

import com.rentsphere.entity.Facility;
import com.rentsphere.entity.Facility.FacilityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FacilityRepository extends JpaRepository<Facility, Long> {

    List<Facility> findByPropertyId(Long propertyId);

    Optional<Facility> findByPropertyIdAndFacilityType(Long propertyId, FacilityType type);

    boolean existsByPropertyIdAndFacilityType(Long propertyId, FacilityType type);
}
