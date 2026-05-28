package com.tooba.EduEvent.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ViolationRequest {
    @NotBlank(message = "Violation type is required")
    private String type; // face_missing, gaze_away, tab_switch
}