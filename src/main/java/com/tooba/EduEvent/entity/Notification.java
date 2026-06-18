package com.tooba.EduEvent.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "notifications")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Notification {
    @Id
    private String id;

    private String userId;

    private String title;
    private String message;

    @Builder.Default
    private Boolean isRead = false;

    @CreatedDate
    private LocalDateTime createdAt;
}
