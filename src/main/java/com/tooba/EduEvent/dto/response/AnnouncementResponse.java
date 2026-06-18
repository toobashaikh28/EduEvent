package com.tooba.EduEvent.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AnnouncementResponse {
    private String id;
    private String authorName;
    private String content;
    private LocalDateTime createdAt;
    private List<CommentResponse> comments;

    @Data
    @Builder
    public static class CommentResponse {
        private String id;
        private String authorName;
        private String content;
        private LocalDateTime createdAt;
    }
}