package com.tooba.EduEvent.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "leaderboard") 
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Leaderboard {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- SHARED FIELDS ---
    @Builder.Default
    @Column(name = "rank")
    private Integer rank = 0;

    @CreationTimestamp
    private LocalDateTime announcedAt;

    // --- HACKATHON TEAM FIELDS ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hackathon_id")
    private Event hackathon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalScore;

    // --- QUIZ USER FIELDS (NEW) ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "score")
    private Double score;
}