package com.rentsphere.service;

import com.rentsphere.dto.request.PropertyRequest;
import com.rentsphere.dto.response.PagedResponse;
import com.rentsphere.dto.response.PropertyResponse;
import com.rentsphere.entity.Property;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

public interface PropertyService {
    PropertyResponse create(PropertyRequest request, Long ownerId);
    PropertyResponse update(Long id, PropertyRequest request, Long requesterId);
    void delete(Long id, Long requesterId);
    PropertyResponse findById(Long id);
    PagedResponse<PropertyResponse> findAll(int page, int size);
    PagedResponse<PropertyResponse> findByOwner(Long ownerId, int page, int size);
    PagedResponse<PropertyResponse> search(
        String name, String city, Property.PropertyType type,
        Property.AvailabilityStatus status,
        BigDecimal minRent, BigDecimal maxRent,
        String sortBy, int page, int size
    );
    PropertyResponse uploadImages(Long propertyId, List<MultipartFile> files, Long requesterId);
    void deleteImage(Long propertyId, Long imageId, Long requesterId);
    PropertyResponse updateStatus(Long id, Property.AvailabilityStatus status, Long requesterId);
    List<String> getAllCities();
}
