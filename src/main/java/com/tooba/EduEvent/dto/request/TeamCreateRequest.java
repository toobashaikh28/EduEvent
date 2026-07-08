package com.tooba.EduEvent.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TeamCreateRequest {
    @NotBlank(message = "Team name is required")
    @Size(min = 3, max = 50, message = "Team name must be between 3 and 50 characters")
    private String teamName;

    private String eventId;

    // FIX (feature list): "Enters team name and max size" — was hardcoded to 4 in the service
    @Min(value = 2, message = "Team max size must be at least 2")
    @Max(value = 10, message = "Team max size cannot exceed 10")
    private Integer maxSize;   // optional — defaults to 4 when omitted
}
