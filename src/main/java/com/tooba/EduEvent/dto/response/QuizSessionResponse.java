package com.tooba.EduEvent.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class QuizSessionResponse {
    private Long sessionId;
    private Integer durationMinutes;
    private LocalDateTime startTime;
    private List<QuestionResponse> questions; // shuffled
}