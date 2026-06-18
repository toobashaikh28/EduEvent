package com.tooba.EduEvent.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "teams")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Team {
    @Id
    private String id;

    private String hackathonId;

    private String name;

    @Indexed(unique = true, sparse = true)
    private String joinCode;

    private String leaderId;

    @Builder.Default
    private Boolean isLocked = false;

    @Builder.Default
    private Integer maxSize = 4;

    @CreatedDate
    private LocalDateTime createdAt;
}
