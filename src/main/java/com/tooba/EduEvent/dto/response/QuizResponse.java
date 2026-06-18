package com.tooba.EduEvent.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QuizResponse {
    private String id;
    private String eventId;
    private Integer durationMinutes;
    private Double passScore;
    private Boolean randomize;
    // Admin list extras
    private String eventTitle;
    private String eventStatus;
    private Integer questionCount;
    private Integer participants;
    private Double passRate;
    private Double avgScore;
}
