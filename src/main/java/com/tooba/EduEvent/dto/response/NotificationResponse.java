package com.tooba.EduEvent.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NotificationResponse {
    private String title;
    private String message;
    private LocalDateTime timestamp;
    private boolean isRead;
}