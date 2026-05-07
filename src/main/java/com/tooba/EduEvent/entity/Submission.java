package com.tooba.EduEvent.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "submissions")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Submission {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne @JoinColumn(name = "hackathon_id", nullable = false)
    private Event hackathon;

    private String title;
    
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String description;

    private String githubUrl;
    private String file_path;

    @CreationTimestamp
    private LocalDateTime submittedAt;
}