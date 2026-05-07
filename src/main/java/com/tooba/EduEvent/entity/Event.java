package com.tooba.EduEvent.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "events")
@Data                // Generates getters, setters, toString, equals, and hashCode
@Builder             // Enables the Builder pattern for object creation
@NoArgsConstructor   // Required by JPA for entity instantiation
@AllArgsConstructor  // Required by Lombok's @Builder
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String type; // Maps to 'Webinar', 'Conference', 'Hackathon', 'Quiz'

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Builder.Default
    private Integer capacity = 100;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Builder.Default
    private String status = "UPCOMING"; // 'UPCOMING', 'LIVE', 'COMPLETED', 'CANCELLED'

    @Column(name = "banner_url", length = 500)
    private String bannerUrl;

    @Column(name = "join_link", length = 500)
    private String joinLink;

    /**
     * Relationship: Many Events are managed by One User (Admin).
     * Maps to 'admin_id' in your SQL table.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private User admin;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}