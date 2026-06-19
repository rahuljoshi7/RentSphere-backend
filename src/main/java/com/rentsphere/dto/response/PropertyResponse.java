package com.rentsphere.dto.response;

import com.rentsphere.entity.Property;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class PropertyResponse {
    private Long id;
    private String name;
    private Property.PropertyType propertyType;
    private String address;
    private String city;
    private String state;
    private BigDecimal rentAmount;
    private BigDecimal depositAmount;
    private Integer numRooms;
    private String description;
    private Property.AvailabilityStatus availabilityStatus;
    private Long ownerId;
    private String ownerName;
    private Long managerId;
    private String managerName;
    private List<PropertyImageResponse> images;
    private Instant createdAt;
    private Instant updatedAt;

    @Data
    @Builder
    public static class PropertyImageResponse {
        private Long id;
        private String imageUrl;
        private Boolean isPrimary;
    }
}
