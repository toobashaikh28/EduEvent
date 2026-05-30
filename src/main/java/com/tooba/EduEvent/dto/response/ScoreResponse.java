package com.tooba.EduEvent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class ScoreResponse {
    private Long id;
    private Long submissionId;
    private String teamName;
    private Long judgeId;
    private String judgeName;
    private Integer scoreValue;
    private String feedback;
    private LocalDateTime scoredAt;
}
