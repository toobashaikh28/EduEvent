package com.tooba.EduEvent.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "team_members")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TeamMember {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder.Default
    private String role = "MEMBER"; // 'LEADER', 'MEMBER'

    @Builder.Default
    private String status = "PENDING"; // 'PENDING', 'ACCEPTED'
}