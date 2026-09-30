package com.tooba.EduEvent.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.UUID;

@Document(collection = "certificates")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Certificate {

    @Id
    private String id;
    @org.springframework.data.mongodb.core.index.Indexed

    private String userId;

    private String eventId;

    @Indexed(unique = true)
    private UUID certUuid;

    private String pdfUrl;

    private String verifyUrl;

    @CreatedDate
    private LocalDateTime issuedAt;
}
