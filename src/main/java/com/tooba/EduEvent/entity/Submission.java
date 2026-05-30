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

    // FIX 4: Renamed from file_path (snake_case field) to filePath (camelCase),
    // explicit @Column keeps the DB column name unchanged.
    @Column(name = "file_path")
    private String filePath;

    @CreationTimestamp
    private LocalDateTime submittedAt;
}
