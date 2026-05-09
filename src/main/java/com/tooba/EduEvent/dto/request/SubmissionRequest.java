package com.tooba.EduEvent.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SubmissionRequest {
    private Long quizId;
    private Long userId;
    
    @NotBlank(message = "Content or URL is required")
    private String content; // Could be answers or a file link
}