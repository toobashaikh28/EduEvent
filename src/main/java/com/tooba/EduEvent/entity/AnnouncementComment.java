package com.tooba.EduEvent.entity;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Embedded document — comments live inside a {@link TeamAnnouncement} document
 * (not a separate collection). The {@code id} and {@code createdAt} are assigned
 * in the service.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AnnouncementComment {

    private String id;

    private String authorId;

    private String content;

    private LocalDateTime createdAt;
}
