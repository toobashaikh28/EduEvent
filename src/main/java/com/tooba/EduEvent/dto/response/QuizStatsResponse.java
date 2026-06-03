package com.tooba.EduEvent.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QuizStatsResponse {
    private Long quizId;
    private Double averageScore;
    private Double passRatePercentage;
}