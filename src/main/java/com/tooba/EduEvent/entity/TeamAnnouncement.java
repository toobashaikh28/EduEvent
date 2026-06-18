package com.tooba.EduEvent.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "team_announcements")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TeamAnnouncement {
    @Id
    private String id;

    private String teamId;

    private String authorId;

    private String content;

    @CreatedDate
    private LocalDateTime createdAt;

    // Comments are embedded inside the announcement document.
    @Builder.Default
    private List<AnnouncementComment> comments = new ArrayList<>();
}
