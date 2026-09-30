package com.tooba.EduEvent.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "leaderboard")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Leaderboard {

    @Id
    private String id;

    // --- SHARED FIELDS ---
    @Builder.Default
    private Integer rank = 0;

    @CreatedDate
    private LocalDateTime announcedAt;

    // --- HACKATHON TEAM FIELDS ---
    @org.springframework.data.mongodb.core.index.Indexed
    private String hackathonId;

    private String teamId;

    private BigDecimal totalScore;

    // --- QUIZ USER FIELDS ---
    @org.springframework.data.mongodb.core.index.Indexed
    private String eventId;
    @org.springframework.data.mongodb.core.index.Indexed

    private String userId;

    private Double score;
}
