package com.tooba.EduEvent.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class QuestionRequest {
    @NotBlank(message = "Question text is required")
    private String questionText;

    private List<String> options;

    @NotBlank(message = "Correct answer is required")
    private String correctAnswer;

    private int points;
}