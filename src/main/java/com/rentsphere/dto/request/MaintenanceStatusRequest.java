package com.rentsphere.dto.request;

import com.rentsphere.entity.MaintenanceRequest.MaintenanceStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MaintenanceStatusRequest {
    @NotNull private MaintenanceStatus status;
    private String notes;
}
