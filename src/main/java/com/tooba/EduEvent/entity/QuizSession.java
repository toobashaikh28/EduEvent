package com.tooba.EduEvent.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "quiz_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizSession {

    @Id
    private String id;
    @org.springframework.data.mongodb.core.index.Indexed

    private String quizId;
    @org.springframework.data.mongodb.core.index.Indexed

    private String userId;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Double score;

    // ONGOING, COMPLETED, INVALIDATED
    @Builder.Default
    private String status = "ONGOING";

    @Builder.Default
    private Integer violationCount = 0;
}
