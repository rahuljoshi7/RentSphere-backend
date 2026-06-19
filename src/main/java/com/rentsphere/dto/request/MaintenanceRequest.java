package com.rentsphere.dto.request;

import com.rentsphere.entity.MaintenanceRequest.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MaintenanceRequest {
    @NotNull  private Long propertyId;
    @NotBlank @Size(max = 200) private String title;
    @NotBlank private String description;
    private Priority priority = Priority.MEDIUM;
}
