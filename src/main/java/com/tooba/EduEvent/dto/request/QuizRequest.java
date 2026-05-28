package com.tooba.EduEvent.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class QuizRequest {

    @NotNull(message = "Event ID is required")
    private Long eventId;

    @Min(value = 1, message = "Duration must be at least 1 minute")
    private Integer durationMinutes = 30;

    private BigDecimal passScore;   // defaults to 50.00 in service if null

    private Boolean randomize;      // defaults to true in service if null
}
