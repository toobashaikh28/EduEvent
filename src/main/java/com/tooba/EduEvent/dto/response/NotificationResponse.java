package com.tooba.EduEvent.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {
    private String id;
    private String title;
    private String message;
    private Boolean isRead;
    private LocalDateTime createdAt;
}