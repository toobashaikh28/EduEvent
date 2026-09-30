package com.tooba.EduEvent.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "events")
@Data                // Generates getters, setters, toString, equals, and hashCode
@Builder             // Enables the Builder pattern for object creation
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    @Id
    private String id;

    private String title;
    @org.springframework.data.mongodb.core.index.Indexed

    private String type; // Maps to 'Webinar', 'Conference', 'Hackathon', 'Quiz'

    private String description;

    @Builder.Default
    private Integer capacity = 100;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @Builder.Default
    @org.springframework.data.mongodb.core.index.Indexed
    private String status = "UPCOMING"; // 'UPCOMING', 'LIVE', 'COMPLETED', 'CANCELLED'

    private String bannerUrl;

    private String joinLink;

    /**
     * Relationship: Many Events are managed by One User (Admin).
     * Stored as the admin's user id (manual reference).
     */
    private String adminId;

    @CreatedDate
    private LocalDateTime createdAt;
}
