package com.tooba.EduEvent.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ScoreRequest {

    @NotNull(message = "Submission ID is required")
    private String submissionId;

    @NotNull(message = "Score value is required")
    @Min(value = 0, message = "Score cannot be negative")
    @Max(value = 100, message = "Score cannot exceed 100")
    private Integer scoreValue;

    private String feedback;
}