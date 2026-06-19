package com.rentsphere.repository;

import com.rentsphere.entity.PropertyImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PropertyImageRepository extends JpaRepository<PropertyImage, Long> {

    List<PropertyImage> findByPropertyId(Long propertyId);

    Optional<PropertyImage> findByPropertyIdAndIsPrimaryTrue(Long propertyId);

    @Modifying
    @Query("UPDATE PropertyImage pi SET pi.isPrimary = false WHERE pi.property.id = :propertyId")
    void clearPrimaryForProperty(@Param("propertyId") Long propertyId);

    void deleteByPropertyId(Long propertyId);

    long countByPropertyId(Long propertyId);
}
