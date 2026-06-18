package com.tooba.EduEvent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class SubmissionResponse {
    private String id;
    private String teamId;
    private String teamName;
    private String hackathonId;
    private String hackathonTitle;
    private String title;
    private String description;
    private String githubUrl;
    private String filePath;
    private LocalDateTime submittedAt;
}