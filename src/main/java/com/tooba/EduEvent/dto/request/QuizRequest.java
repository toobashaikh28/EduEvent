package com.tooba.EduEvent.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class QuizRequest {
    @NotBlank(message = "Quiz title is required")
    private String title;

    @Min(value = 1, message = "Duration must be at least 1 minute")
    private int durationMinutes;

    private Long eventId; // To link the quiz to an event
}