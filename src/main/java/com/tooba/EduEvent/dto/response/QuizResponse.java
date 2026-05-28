package com.tooba.EduEvent.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QuizResponse {
    private Long id;
    private Long eventId;
    private Integer durationMinutes;
    private Double passScore;
    private Boolean randomize;
}