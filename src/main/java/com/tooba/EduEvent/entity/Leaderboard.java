package com.tooba.EduEvent.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "leaderboard",
       uniqueConstraints = @UniqueConstraint(columnNames = {"hackathon_id", "team_id"}))
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Leaderboard {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hackathon_id", nullable = false)
    private Event hackathon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(nullable = false)
    private Integer rank;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalScore;

    @CreationTimestamp
    private LocalDateTime announcedAt;
}