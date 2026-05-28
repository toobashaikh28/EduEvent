package com.tooba.EduEvent.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminQuizResultResponse {
    private Long sessionId;
    private String userName;
    private Double score;
    private String status;
    private Integer violationCount;
}