package com.tooba.EduEvent.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "teams")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Team {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hackathon_id", nullable = false)
    private Event hackathon;

    @Column(nullable = false)
    private String name;

    @Column(name = "join_code", unique = true, length = 10)
    private String joinCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leader_id", nullable = false)
    private User leader;

    @Builder.Default
    private Boolean isLocked = false;

    @Builder.Default
    private Integer maxSize = 4;

    @CreationTimestamp
    private LocalDateTime createdAt;
}