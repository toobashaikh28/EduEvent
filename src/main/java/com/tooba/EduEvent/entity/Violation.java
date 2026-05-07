package com.tooba.EduEvent.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "violations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Violation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private QuizSession session;

    @Column(nullable = false)
    private String type; // e.g., 'TAB_SWITCH', 'FULLSCREEN_EXIT'

    @CreationTimestamp
    @Column(name = "violation_time")
    private LocalDateTime violationTime;
}