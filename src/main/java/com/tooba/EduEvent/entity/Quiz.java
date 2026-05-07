package com.tooba.EduEvent.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "quizzes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Quiz {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "event_id", nullable = false, unique = true)
    private Event event;

    @Builder.Default
    @Column(name = "duration_minutes")
    private Integer durationMinutes = 30;

    @Builder.Default
    @Column(name = "pass_score", precision = 5, scale = 2)
    private java.math.BigDecimal passScore = new java.math.BigDecimal("50.00");

    @Builder.Default
    private Boolean randomize = true;
}