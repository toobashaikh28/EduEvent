package com.tooba.EduEvent.dto.request;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class ScoreRequest {
    private Long submissionId;

    @Min(value = 0, message = "Score cannot be negative")
    private int scoreValue;

    private String feedback;
}