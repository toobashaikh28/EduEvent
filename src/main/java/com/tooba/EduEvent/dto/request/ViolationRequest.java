package com.tooba.EduEvent.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ViolationRequest {
    private Long userId;
    
    @NotBlank(message = "Violation type is required")
    private String type; // e.g., "TAB_SWITCH", "FACE_NOT_DETECTED"

    private String description;
}