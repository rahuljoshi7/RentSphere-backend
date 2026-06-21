package com.rentsphere.service;

import com.rentsphere.dto.request.PropertyRequest;
import com.rentsphere.dto.response.PagedResponse;
import com.rentsphere.dto.response.PropertyResponse;
import com.rentsphere.entity.Property;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

public interface PropertyService {

    // ==================================================
    // PROPERTY CRUD
    // ==================================================

    PropertyResponse create(
            PropertyRequest request,
            Long ownerId
    );

    PropertyResponse update(
            Long propertyId,
            PropertyRequest request,
            Long requesterId
    );

    void delete(
            Long propertyId,
            Long requesterId
    );

    PropertyResponse findById(
            Long propertyId
    );

    PagedResponse<PropertyResponse> findAll(
            int page,
            int size
    );

    PagedResponse<PropertyResponse> findByOwner(
            Long ownerId,
            int page,
            int size
    );

    // ==================================================
    // SEARCH & FILTER
    // ==================================================

    PagedResponse<PropertyResponse> search(
            String name,
            String city,
            Property.PropertyType type,
            Property.AvailabilityStatus status,
            BigDecimal minRent,
            BigDecimal maxRent,
            String sortBy,
            int page,
            int size
    );

    List<String> getAllCities();

    // ==================================================
    // PROPERTY IMAGES
    // ==================================================

    PropertyResponse uploadImages(
            Long propertyId,
            List<MultipartFile> files,
            Long requesterId
    );

    void deleteImage(
            Long propertyId,
            Long imageId,
            Long requesterId
    );

    // ==================================================
    // PROPERTY STATUS
    // ==================================================

    PropertyResponse updateStatus(
            Long propertyId,
            Property.AvailabilityStatus status,
            Long requesterId
    );
}
