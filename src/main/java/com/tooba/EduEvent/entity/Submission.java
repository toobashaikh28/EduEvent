package com.tooba.EduEvent.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "submissions")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Submission {

    @Id
    private String id;
    @org.springframework.data.mongodb.core.index.Indexed

    private String teamId;
    @org.springframework.data.mongodb.core.index.Indexed

    private String hackathonId;

    private String title;

    private String description;

    private String githubUrl;

    private String filePath;

    @CreatedDate
    private LocalDateTime submittedAt;
}
