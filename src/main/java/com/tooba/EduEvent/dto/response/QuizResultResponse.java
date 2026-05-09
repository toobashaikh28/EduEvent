package com.tooba.EduEvent.dto.response;

import lombok.Data;

@Data
public class QuizResultResponse {
    private String quizTitle;
    private int totalScore;
    private int obtainedMarks;
    private String feedback;
    private boolean passed;
}