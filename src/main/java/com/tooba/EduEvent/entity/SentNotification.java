package com.tooba.EduEvent.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * A log of broadcasts an admin has sent — one record per send action
 * (not per recipient). Powers the admin "Sent History" panel.
 */
@Document(collection = "sent_notifications")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SentNotification {
    @Id
    private String id;

    private String senderId;     // the admin who sent it

    private String title;
    private String message;

    private String target;       // all|participants|judges|admins, or a specific email
    private int recipientCount;  // how many in-app notifications were delivered
    private int emailedCount;    // how many emails went out

    @CreatedDate
    private LocalDateTime createdAt;
}
