package com.rentsphere.dto.request;

import com.rentsphere.entity.Property;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PropertyRequest {

    @NotBlank @Size(max = 200)
    private String name;

    @NotNull
    private Property.PropertyType propertyType;

    @NotBlank
    private String address;

    @NotBlank @Size(max = 100)
    private String city;

    @NotBlank @Size(max = 100)
    private String state;

    @NotNull @DecimalMin("0.01")
    private BigDecimal rentAmount;

    @NotNull @DecimalMin("0.00")
    private BigDecimal depositAmount;

    @NotNull @Min(1)
    private Integer numRooms;

    private String description;

    private Long managerId;
}
