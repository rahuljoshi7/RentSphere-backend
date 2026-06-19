package com.rentsphere.service.impl;

import com.rentsphere.dto.request.PropertyRequest;
import com.rentsphere.dto.response.PagedResponse;
import com.rentsphere.dto.response.PropertyResponse;
import com.rentsphere.entity.Property;
import com.rentsphere.entity.PropertyImage;
import com.rentsphere.entity.User;
import com.rentsphere.exception.ResourceNotFoundException;
import com.rentsphere.exception.UnauthorizedException;
import com.rentsphere.repository.PropertyImageRepository;
import com.rentsphere.repository.PropertyRepository;
import com.rentsphere.repository.UserRepository;
import com.rentsphere.service.CloudinaryService;
import com.rentsphere.service.PropertyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PropertyServiceImpl implements PropertyService {

    private final PropertyRepository      propertyRepository;
    private final PropertyImageRepository imageRepository;
    private final UserRepository          userRepository;
    private final CloudinaryService       cloudinaryService;

    // ── Create ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public PropertyResponse create(PropertyRequest request, Long ownerId) {
        User owner = findUserOrThrow(ownerId);
        User manager = request.getManagerId() != null
            ? findUserOrThrow(request.getManagerId()) : null;

        Property property = Property.builder()
            .owner(owner)
            .manager(manager)
            .name(request.getName())
            .propertyType(request.getPropertyType())
            .address(request.getAddress())
            .city(request.getCity())
            .state(request.getState())
            .rentAmount(request.getRentAmount())
            .depositAmount(request.getDepositAmount())
            .numRooms(request.getNumRooms())
            .description(request.getDescription())
            .availabilityStatus(Property.AvailabilityStatus.AVAILABLE)
            .build();

        property = propertyRepository.save(property);
        log.info("Property created: id={} by owner={}", property.getId(), ownerId);
        return toResponse(property);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public PropertyResponse update(Long id, PropertyRequest request, Long requesterId) {
        Property property = findPropertyOrThrow(id);
        assertOwnerOrManager(property, requesterId);

        User manager = request.getManagerId() != null
            ? findUserOrThrow(request.getManagerId()) : null;

        property.setManager(manager);
        property.setName(request.getName());
        property.setPropertyType(request.getPropertyType());
        property.setAddress(request.getAddress());
        property.setCity(request.getCity());
        property.setState(request.getState());
        property.setRentAmount(request.getRentAmount());
        property.setDepositAmount(request.getDepositAmount());
        property.setNumRooms(request.getNumRooms());
        property.setDescription(request.getDescription());

        return toResponse(propertyRepository.save(property));
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void delete(Long id, Long requesterId) {
        Property property = findPropertyOrThrow(id);
        assertOwner(property, requesterId);

        // Remove all images from Cloudinary
        imageRepository.findByPropertyId(id)
            .forEach(img -> cloudinaryService.deleteImage(img.getCloudinaryPublicId()));

        propertyRepository.delete(property);
        log.info("Property deleted: id={}", id);
    }

    // ── Find ──────────────────────────────────────────────────────────────────

    @Override
    public PropertyResponse findById(Long id) {
        return toResponse(findPropertyOrThrow(id));
    }

    @Override
    public PagedResponse<PropertyResponse> findAll(int page, int size) {
        Page<Property> result = propertyRepository.findAll(pageOf(page, size, "createdAt"));
        return PagedResponse.of(result.map(this::toResponse));
    }

    @Override
    public PagedResponse<PropertyResponse> findByOwner(Long ownerId, int page, int size) {
        Page<Property> result = propertyRepository.findByOwnerId(ownerId, pageOf(page, size, "createdAt"));
        return PagedResponse.of(result.map(this::toResponse));
    }

    // ── Search & Filter ───────────────────────────────────────────────────────

    @Override
    public PagedResponse<PropertyResponse> search(
        String name, String city, Property.PropertyType type,
        Property.AvailabilityStatus status,
        BigDecimal minRent, BigDecimal maxRent,
        String sortBy, int page, int size
    ) {
        Sort sort = switch (sortBy == null ? "" : sortBy) {
            case "rentAsc"  -> Sort.by("rentAmount").ascending();
            case "rentDesc" -> Sort.by("rentAmount").descending();
            default         -> Sort.by("createdAt").descending();
        };
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Property> result = propertyRepository.searchAndFilter(
            name, city, type, status, minRent, maxRent, pageable
        );
        return PagedResponse.of(result.map(this::toResponse));
    }

    // ── Images ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public PropertyResponse uploadImages(Long propertyId, List<MultipartFile> files, Long requesterId) {
        Property property = findPropertyOrThrow(propertyId);
        assertOwnerOrManager(property, requesterId);

        boolean hasNoPrimary = imageRepository.findByPropertyIdAndIsPrimaryTrue(propertyId).isEmpty();

        for (int i = 0; i < files.size(); i++) {
            CloudinaryService.UploadResult result =
                cloudinaryService.uploadImage(files.get(i), "properties/" + propertyId);

            PropertyImage image = PropertyImage.builder()
                .property(property)
                .imageUrl(result.url())
                .cloudinaryPublicId(result.publicId())
                .isPrimary(hasNoPrimary && i == 0)   // First uploaded image becomes primary
                .build();

            imageRepository.save(image);
        }

        return toResponse(findPropertyOrThrow(propertyId));
    }

    @Override
    @Transactional
    public void deleteImage(Long propertyId, Long imageId, Long requesterId) {
        Property property = findPropertyOrThrow(propertyId);
        assertOwnerOrManager(property, requesterId);

        PropertyImage image = imageRepository.findById(imageId)
            .filter(img -> img.getProperty().getId().equals(propertyId))
            .orElseThrow(() -> new ResourceNotFoundException("PropertyImage", imageId));

        cloudinaryService.deleteImage(image.getCloudinaryPublicId());
        imageRepository.delete(image);

        // Promote another image to primary if the deleted one was primary
        if (Boolean.TRUE.equals(image.getIsPrimary())) {
            imageRepository.findByPropertyId(propertyId).stream()
                .findFirst()
                .ifPresent(next -> {
                    next.setIsPrimary(true);
                    imageRepository.save(next);
                });
        }
    }

    // ── Status ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public PropertyResponse updateStatus(Long id, Property.AvailabilityStatus status, Long requesterId) {
        Property property = findPropertyOrThrow(id);
        assertOwnerOrManager(property, requesterId);
        property.setAvailabilityStatus(status);
        return toResponse(propertyRepository.save(property));
    }

    @Override
    public List<String> getAllCities() {
        return propertyRepository.findAllCities();
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private PropertyResponse toResponse(Property p) {
        List<PropertyResponse.PropertyImageResponse> images = imageRepository
            .findByPropertyId(p.getId()).stream()
            .map(img -> PropertyResponse.PropertyImageResponse.builder()
                .id(img.getId())
                .imageUrl(img.getImageUrl())
                .isPrimary(img.getIsPrimary())
                .build())
            .collect(Collectors.toList());

        return PropertyResponse.builder()
            .id(p.getId())
            .name(p.getName())
            .propertyType(p.getPropertyType())
            .address(p.getAddress())
            .city(p.getCity())
            .state(p.getState())
            .rentAmount(p.getRentAmount())
            .depositAmount(p.getDepositAmount())
            .numRooms(p.getNumRooms())
            .description(p.getDescription())
            .availabilityStatus(p.getAvailabilityStatus())
            .ownerId(p.getOwner().getId())
            .ownerName(p.getOwner().getFullName())
            .managerId(p.getManager() != null ? p.getManager().getId() : null)
            .managerName(p.getManager() != null ? p.getManager().getFullName() : null)
            .images(images)
            .createdAt(p.getCreatedAt())
            .updatedAt(p.getUpdatedAt())
            .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Property findPropertyOrThrow(Long id) {
        return propertyRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Property", id));
    }

    private User findUserOrThrow(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private void assertOwner(Property property, Long requesterId) {
        if (!property.getOwner().getId().equals(requesterId)) {
            throw new UnauthorizedException("Only the property owner can perform this action.");
        }
    }

    private void assertOwnerOrManager(Property property, Long requesterId) {
        boolean isOwner   = property.getOwner().getId().equals(requesterId);
        boolean isManager = property.getManager() != null
            && property.getManager().getId().equals(requesterId);
        if (!isOwner && !isManager) {
            throw new UnauthorizedException("Access denied for this property.");
        }
    }

    private Pageable pageOf(int page, int size, String sortField) {
        return PageRequest.of(page, size, Sort.by(sortField).descending());
    }
}
