package com.tooba.EduEvent.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "judge_assignments",
       uniqueConstraints = @UniqueConstraint(columnNames = {"judge_id", "hackathon_id"}))
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class JudgeAssignment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "judge_id", nullable = false)
    private User judge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hackathon_id", nullable = false)
    private Event hackathon;

    @CreationTimestamp
    private LocalDateTime assignedAt;
}